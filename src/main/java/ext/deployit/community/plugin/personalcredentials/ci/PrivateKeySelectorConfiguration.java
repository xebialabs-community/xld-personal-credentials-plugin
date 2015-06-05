/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.plugin.personalcredentials.ci;

import static com.google.common.collect.Lists.newArrayList;

import java.util.HashMap;
import java.util.Map;

import com.xebialabs.deployit.plugin.api.udm.Metadata;
import com.xebialabs.deployit.plugin.api.udm.Property;
import com.xebialabs.deployit.plugin.api.udm.base.BaseConfigurationItem;

@Metadata(root = Metadata.ConfigurationItemRoot.CONFIGURATION, description = "Configuration of PrivateKeySelector (personal-credentials)")
public class PrivateKeySelectorConfiguration extends BaseConfigurationItem {
	
	private static final long serialVersionUID = 4904932535274939669L;

	@Property(description = "Map a user-defined id with a private key file path on XLD server", required=false, label="Private keys ids and files")
	private Map<String, String> privateKeyIds = new HashMap<String, String>();

	public Map<String, String> getPrivateKeyIds(){
		return privateKeyIds;
	}
	
}
