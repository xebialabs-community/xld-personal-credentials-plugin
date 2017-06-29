/**
 * Copyright 2017 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ext.deployit.community.plugin.personalcredentials.contributor;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.xebialabs.deployit.engine.spi.exception.DeployitException;
import com.xebialabs.deployit.plugin.api.deployment.planning.PrePlanProcessor;
import com.xebialabs.deployit.plugin.api.deployment.specification.Delta;
import com.xebialabs.deployit.plugin.api.deployment.specification.DeltaSpecification;
import com.xebialabs.deployit.plugin.api.flow.Step;
import com.xebialabs.deployit.plugin.api.reflect.Descriptor;
import com.xebialabs.deployit.plugin.api.reflect.PropertyDescriptor;
import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.deployit.plugin.api.udm.Deployed;
import com.xebialabs.deployit.plugin.api.udm.DeployedApplication;
import com.xebialabs.deployit.plugin.api.udm.Environment;
import com.xebialabs.deployit.plugin.overthere.Host;
import com.xebialabs.deployit.plugin.overthere.HostContainer;
import com.xebialabs.deployit.plugin.overthere.step.CheckCommandExecutionStep;

import ext.deployit.community.plugin.personalcredentials.ci.CredentialsType;
import ext.deployit.community.plugin.personalcredentials.ci.PrivateKeySelectorConfiguration;
import ext.deployit.community.plugin.personalcredentials.script.ScriptRunner;

public class IdentityContributor {

	@PrePlanProcessor
    static public List<Step> injectPersonalCredentials(DeltaSpecification specification) {
    	logger.trace("injectPersonalCredentials()");
    	
    	final List<Delta> deltas = specification.getDeltas();
        final DeployedApplication deployedApplication = specification.getDeployedApplication();
        final Environment environment = deployedApplication.getEnvironment();

        Boolean override = environment.getProperty("overrideHostCredentials");
        if (!override)
            return null;

        final Set<Host> hosts = ImmutableSet.<Host>builder()
                .addAll(filter(transform(deltas, DEPLOYED_TO_HOST), notNull()))
                .addAll(filter(transform(deltas, PREVIOUS_TO_HOST), notNull()))
                .build();

        logger.debug("Hosts {}", hosts);
        
        CredentialsType  credentialType = deployedApplication.<CredentialsType>getProperty("credentialsType");
        logger.debug("CredentialsType {}", credentialType);
        
        switch (credentialType){
        	case CT_USERNAME            : return injectUsernames(hosts, deployedApplication); 	
        	case CT_PRIVATEKEY          : return injectSshKeys(hosts, deployedApplication, false);
        	case CT_PRIVATEKEY_SELECTOR : return injectSshKeys(hosts, deployedApplication, true);
        	case CT_CUSTOM_SCRIPT       : return executeCustomScript(hosts, deployedApplication); 
        	default                     : return null;
        }	
        	
    }
    
    protected static List<Step> executeCustomScript(Set<Host> hosts, final DeployedApplication deployedApplication){
    	if (!deployedApplication.hasProperty("scriptPath"))
    		throw new RuntimeException("Missing scriptPath property");
    	String scriptPath = deployedApplication.getProperty("scriptPath");
    	String scriptClasspath = "";
    	if (deployedApplication.hasProperty("scriptClasspath")){
    		scriptClasspath = deployedApplication.getProperty("scriptClasspath");
    	}
    	ScriptRunner.executeScript(deployedApplication, deployedApplication.getEnvironment(), hosts, scriptPath, scriptClasspath);
    	final Iterable<List<Step>> transform = transform(hosts, new Function<Host, List<Step>>() {
    		@Override
            public List<Step> apply(final Host host) {
    			if (!deployedApplication.hasProperty("checkConnection")) {
                    return null;
                }
    			final Boolean checkConnection = deployedApplication.getProperty("checkConnection");
                return (checkConnection ? Collections.singletonList(new CheckCommandExecutionStep(host)) : Collections.EMPTY_LIST);
    		}	
    	});	
    	return newArrayList(concat(transform));
    }
    
    protected static List<Step> injectSshKeys(Set<Host> hosts, final DeployedApplication deployedApplication, boolean performKeyLookup){
    	logger.trace("injectSshKeys()");
    	final Environment environment = deployedApplication.getEnvironment();
    	String privateKeyFileName = deployedApplication.getProperty("privateKey");
    	
    	if (privateKeyFileName==null){
    		throw new DeployitException("Please set a value for the privateKey property");
    	}
    	
    	if (performKeyLookup){
    		if (!environment.hasProperty("privateKeySelectorConfiguration")){
    			throw new RuntimeException("Property privateKeySelectorConfiguration is not defined on environment");
    		}
    		PrivateKeySelectorConfiguration configuration = environment.<PrivateKeySelectorConfiguration>getProperty("privateKeySelectorConfiguration");
    		if (configuration==null){
    			throw new RuntimeException("Property privateKeySelectorConfiguration is not set on environment");
    		}
    		String privateKeyId = deployedApplication.getProperty("privateKey");
    		String keyFileName = configuration.getPrivateKeyIds().get(privateKeyId);
    		if (keyFileName==null){
    			throw new RuntimeException(format("Key selector not found or returned null value : %s", privateKeyId));
    		}
    		privateKeyFileName = keyFileName;
    	}
    	final String privateKey = privateKeyFileName;
    	
    	if (!fileExists(privateKeyFileName)){
    		throw new DeployitException(String.format("File not found : %s",privateKeyFileName));
    	}
    	
    	final Iterable<List<Step>> transform = transform(hosts, new Function<Host, List<Step>>() {
    		@Override
            public List<Step> apply(final Host host) {
    			setSshKey(host, "privateKey", privateKey, "passphrase", deployedApplication);
    			if (!deployedApplication.hasProperty("checkConnection")) {
                    return null;
                }
    			final Boolean checkConnection = deployedApplication.getProperty("checkConnection");
                return (checkConnection ? Collections.singletonList(new CheckCommandExecutionStep(host)) : Collections.EMPTY_LIST);
    		}	
    	});	
    	
    	return newArrayList(concat(transform));
    }	
        
    protected static List<Step> injectUsernames(Set<Host> hosts, final DeployedApplication deployedApplication){
    	logger.trace("injectUsernames()");
    	final Boolean perOsCredential = isPerOsCredential(deployedApplication);

        final Iterable<List<Step>> transform = transform(hosts, new Function<Host, List<Step>>() {
            @Override
            public List<Step> apply(final Host host) {
                if (perOsCredential) {
                    switch (host.getOs()) {
                        case WINDOWS:
                            logger.debug("IdentityContributor injects credentials in a {} host {}", "WINDOWS", host.getId());
                            setCredentials(host, "windowsUsername", "windowsPassword", deployedApplication);
                            break;
                        case UNIX:
                            logger.debug("IdentityContributor injects credentials in a {} host {}", "UNIX", host.getId());
                            setCredentials(host, "unixUsername", "unixPassword",deployedApplication);
                            break;
                    }
                } else {
                    logger.debug("IdentityContributor injects credentials in a host {} ", host.getId());
                    setCredentials(host, "username", "password", deployedApplication);
                }

                if (!deployedApplication.hasProperty("checkConnection")) {
                    return null;
                }
                final Boolean checkConnection = deployedApplication.getProperty("checkConnection");
                return (checkConnection ? Collections.singletonList(new CheckCommandExecutionStep(host)) : Collections.EMPTY_LIST);
            }
        }   
        );
        return newArrayList(concat(transform));
       } 
       
    private static void setSshKey(final Host host, final String privateKeyFilePropertyName, final String privateKeyValue, final String passphrasePropertyName, final DeployedApplication deployedApplication) {
    	logger.trace("setSshKey()");
    	final String privateKeyFile = privateKeyValue;
        final String passphrase = deployedApplication.getProperty(passphrasePropertyName);
                
        if (Strings.isNullOrEmpty(privateKeyFile) || Strings.isNullOrEmpty(passphrase)) {
            final Descriptor descriptor = deployedApplication.getType().getDescriptor();
            final String privateKeyLabel = descriptor.getPropertyDescriptor(privateKeyFilePropertyName).getLabel();
            final String passphraseLabel = descriptor.getPropertyDescriptor(passphrasePropertyName).getLabel();
            throw new RuntimeException(format("Cannot find personal credentials for host (%s/%s), please provide values for the '%s' and '%s' properties",
                    host.getId(),
                    host.getOs().toString(),
                    privateKeyLabel, passphraseLabel
            ));
        }
        host.setProperty("privateKeyFile", privateKeyFile);
        host.setProperty("passphrase", passphrase);
       }
        
        private static void setCredentials(final Host host, final String usernamePropertyName, final String passwordPropertyName, final DeployedApplication deployedApplication) {
                final String username = deployedApplication.getProperty(usernamePropertyName);
                final String password = deployedApplication.getProperty(passwordPropertyName);

                if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
                    final Descriptor descriptor = deployedApplication.getType().getDescriptor();
                    final String usernameLabel = descriptor.getPropertyDescriptor(usernamePropertyName).getLabel();
                    final String passwordLabel = descriptor.getPropertyDescriptor(passwordPropertyName).getLabel();
                    throw new RuntimeException(format("Cannot find personal credentials for host (%s/%s), please provide values for the '%s' and '%s' properties",
                            host.getId(),
                            host.getOs().toString(),
                            usernameLabel, passwordLabel
                    ));
                }
                host.setProperty("username", username);
                host.setProperty("password", password);
        }

    private static Boolean isPerOsCredential(final DeployedApplication deployedApplication) {
        if (deployedApplication.hasProperty("unixUsername") && deployedApplication.hasProperty("unixPassword") &&
                deployedApplication.hasProperty("windowsUsername") && deployedApplication.hasProperty("windowsPassword")) {
            return true;
        }
        if (deployedApplication.hasProperty("username") && deployedApplication.hasProperty("password")) {
            return false;
        }
        throw new RuntimeException("Invalid configuration on udm.DeployedApplication for personal-credentials plugin"
                + ", either set 'username' & 'password' properties or "
                + ", either set 'unixUsername' & 'unixPassword' & 'windowsUsername' & 'windowsPassword' properties.");
    }
    

    private static final Function<Delta, Host> DEPLOYED_TO_HOST = new ToHost() {
        public Host apply(Delta input) {
            return toHost(input.getDeployed());
        }
    };

    private static final Function<Delta, Host> PREVIOUS_TO_HOST = new ToHost() {
        public Host apply(Delta input) {
            return toHost(input.getPrevious());
        }
    };

    static abstract class ToHost implements Function<Delta, Host> {
        protected Host toHost(Deployed<?, ?> deployed) {
            if (deployed == null) {
                return null;
            }
            return toHost(deployed.getContainer());
        }

        private Host toHost(final ConfigurationItem item) {
            if (item instanceof Host) {
                return (Host) item;
            }
            if (item instanceof HostContainer) {
                HostContainer hostContainer = (HostContainer) item;
                return hostContainer.getHost();
            }
            final Collection<PropertyDescriptor> propertyDescriptors = item.getType().getDescriptor().getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getReferencedType() == null)
                    continue;
                if (propertyDescriptor.getReferencedType().instanceOf(Type.valueOf(Host.class))
                        || propertyDescriptor.isAsContainment()) {
                    final Host host = toHost((ConfigurationItem) propertyDescriptor.get(item));
                    if (host != null)
                        return host;
                }
            }
            return null;
        }
    }
    
    protected static boolean fileExists(String fileName){
    	File f = new File(fileName); 
    	return f.exists() & f.isFile();
    }
    
    
    
    protected static final Logger logger = LoggerFactory.getLogger(IdentityContributor.class);
}
