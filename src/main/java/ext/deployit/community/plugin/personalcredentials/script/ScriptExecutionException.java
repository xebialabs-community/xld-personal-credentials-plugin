/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.plugin.personalcredentials.script;

import com.xebialabs.deployit.engine.spi.exception.DeployitException;

@SuppressWarnings("serial")
/*  HttpResponseCodeResult(statusCode = 403) */
public class ScriptExecutionException extends DeployitException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ScriptExecutionException(String msg) {
        super(msg);
    }

    public ScriptExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

