#
# THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
# FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
#

#This is a vanilla script

logger.info("setcredentials.py")
logger.info("DeployedApplication id %s"%deployedApplication.id)
logger.info("Environment id %s"%environment)
for h in hosts:
	logger.info("Hosts used by deployment %s (%s)"%(h.name, h.getProperty("address")))
	#
	#Put your credential lookup and settings below for each host
	#
	#h.setProperty("username","myUsername")
	#h.setProperty("password","myPassword")
logger.info("this is the end")