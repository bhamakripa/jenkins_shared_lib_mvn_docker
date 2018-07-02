import jenkins.model.*

def call(Map parameters = [:]) {

	def stepName = 'dockerCmdExecute'

	echo "[${stepName}] entry point:  : Parameters: ${parameters} "

	handleErrors(stepName: stepName, stepParameters: parameters){

		def dockerImage = parameters.dockerImage ?: ''

		if(dockerImage) {

			try {
				isDockerInstalled(stepName)

				isDockerDaemonRunning(stepName)

				String dockerImageVersion = getDockerImageVersion(stepName, parameters)

				pullDockerImageIfNeeded(stepName, dockerImageVersion)

				executeMavenCommandWithinDockerContainer(stepName, parameters)

				echo "[${stepName}] Maven command execution within docker container fully successful"
				
			} catch (error) {

				echo "[${stepName}] Error occured while executing maven command within docker container. Existing : ${error}"
				throw error
			} finally {
				// 	TODO get container id
				//	TODO docker stop
				//	TODO docker rm

				echo "[${stepName}] finally block: Docker execution completed"
			}
		}
	}
}


private isDockerInstalled(stepName){
	echo "[${stepName}] Check docker is installed "
	def returnCode = sh script: 'which docker > /dev/null', returnStatus: true
	if(returnCode != 0) {
		echo "[${stepName}] Docker is not installed. Exiting."
		throw new Exception("Docker is not installed. Exiitng.")
	}
}


private isDockerDaemonRunning(stepName){
	echo "[${stepName}] Check docker daemon is running."
	returnCode = sh script: 'docker ps -q > /dev/null', returnStatus: true
	if(returnCode != 0) {
		echo "[${stepName}] Cannot connect to docker daemon. Exiting."
		throw new Exception("Docker daemon is not running. Exiitng.")
	}
}

private getDockerImageVersion(stepName, parameters){
	String dockerImageVersion = sh(script: "echo ${parameters.dockerImage} | cut -d ':' -f 2", returnStdout: true, returnStatus: false).trim()
	echo "[${stepName}] dockerImageTag : ${dockerImageVersion}"
	return dockerImageVersion;
}

private pullDockerImageIfNeeded(stepName, dockerImageVersion){
	int dockerImageCount = sh(script: "docker images | grep ${dockerImageVersion} | grep maven | wc -l", returnStdout: true, returnStatus: false).trim().toInteger()
	echo "[${stepName}]  dockerImageCount : ${dockerImageCount}"

	if(dockerImageCount < 1) {
		echo "[${stepName}]  Need to pulll docker image from docker hub"
		returnCode = sh(script: "docker pull ${parameters.dockerImage}", returnStdout: false, returnStatus: true)
		if(returnCode != 0) {
			echo "[${stepName}]  Issues while pulling image from hub. exiting."
			throw new Exception("Issues while pulling image from Docker Hub. Exiitng.")
		}
	}
}

private executeMavenCommandWithinDockerContainer(stepName, Map parameters){
	def dockerRunCmd = "docker run -t --rm --name maven-project -v \${HOME}/.m2:/root/.m2" +
			" -v \$(pwd):/usr/src/mymaven -w /usr/src/mymaven ${parameters.dockerImage}" +
			" ${parameters.mavenCommand} "

	echo "[${stepName}] dockerRunCmd : ${dockerRunCmd}"
	returnCode = sh(script: "${dockerRunCmd}", returnStdout: false, returnStatus: true)
	if(returnCode != 0) {
		echo "[${stepName}] Docker execution failure. exiting."
		throw new Exception("Issues while executing maven command within docker container.")
	}
}

