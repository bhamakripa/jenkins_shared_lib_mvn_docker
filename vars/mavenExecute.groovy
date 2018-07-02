def call(Map inputBuildParameters = [:]) {

	handlePipelineStepErrors(stepName: 'mavenExecute', stepParameters: inputBuildParameters) {

		Set stepGlobalConfigKeys = ['dockerImage', 'globalSettingsFile', 'projectSettingsFile', 'pomPath', 'm2Path']

		Set buildParameterKeys = ['dockerImage', 'globalSettingsFile', 'projectSettingsFile', 'pomPath', 'm2Path', 'flags', 'goals', 'defines', 'logSuccessfulMavenTransfers']

		String mavenCommand = prepareMavenCommandFronBuildParams(inputBuildParameters)

		Map globalStepConfigs  = loadGlobalStepConfigs()
		def dockerImage = getDockerImageName(inputBuildParameters, globalStepConfigs)
		mavenCommand = overwriteGlobalConfigsWithInputParameters(inputBuildParameters, globalStepConfigs, mavenCommand)
		mavenCommand = addMavenGoalsToCommand(inputBuildParameters, mavenCommand);

		echo "mavenExecute : final command : ${mavenCommand}"

		dockerExecute(dockerImage: dockerImage, mavenCommand: mavenCommand)
	}
}

private prepareMavenCommandFronBuildParams(inputBuildParameters){

	String command = "mvn"

	def mavenFlags = inputBuildParameters.flags
	if (mavenFlags?.trim()) {
		command += " ${mavenFlags}"
	}

	// Always use batch mode
	if (!(command.contains('-B') || command.contains('--batch-mode'))){
		command += ' --batch-mode'
	}

	// Disable log for successful transfers by default.
	final String disableSuccessfulMavenTransfersLogFlag = ' -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
	if (!inputBuildParameters.logSuccessfulMavenTransfers.toBoolean()) {
		if (!command.contains(disableSuccessfulMavenTransfersLogFlag)) {
			command += disableSuccessfulMavenTransfersLogFlag
		}
	}

	def defines = inputBuildParameters.defines
	if (defines?.trim()){
		command += " ${defines}"
	}

	return command
}

private addMavenGoalsToCommand(inputBuildParameters, mavenCommand) {
	def mavenGoals = inputBuildParameters.goals
	if (mavenGoals?.trim()) {
		mavenCommand += " ${mavenGoals}"
	}
	return mavenCommand
}


private getDockerImageName(inputBuildParameters, globalStepConfigs) {

	String dockerImageI = getStrValue(inputBuildParameters.dockerImage)
	String dockerImageG = getStrValue(globalStepConfigs.dockerImage)

	if(!dockerImageI.equals("")) {
		return dockerImageI
	} else {
		return dockerImageG
	}
}

private overwriteGlobalConfigsWithInputParameters(inputBuildParameters, globalStepConfigs, mavenCommand){

	/* 2. globalSettingsFile */
	String globalSettingsFile
	globalSettingsFile = "settings.xml"
	String globalSettingsFileI = getStrValue(inputBuildParameters.globalSettingsFile)
	String globalSettingsFileG = getStrValue(globalStepConfigs.globalSettingsFile)

	if(globalSettingsFileI.equals("") && globalSettingsFileG.equals("")) {

	} else if (!globalSettingsFileI.equals("")) {
		globalSettingsFile = globalSettingsFileI
		mavenCommand += " --global-settings '${globalSettingsFileI}'"
	} else if (!globalSettingsFileG.equals("")) {
		globalSettingsFile = globalSettingsFileG
		mavenCommand += " --global-settings '${globalSettingsFileG}'"
	}
	if(globalSettingsFile.startsWith("http")){
		downloadSettingsFromUrl(globalSettingsFile)
	}


	/* 3. projectSettingsFile */
	String projectSettingsFile
	projectSettingsFile = "settings.xml"
	String projectSettingsFileI = getStrValue(inputBuildParameters.projectSettingsFile)
	String projectSettingsFileG = getStrValue(globalStepConfigs.projectSettingsFile)

	if(projectSettingsFileI.equals("") && projectSettingsFileG.equals("")) {

	} else if (!projectSettingsFileI.equals("")) {
		projectSettingsFile = projectSettingsFileI
		mavenCommand += " --settings '${projectSettingsFileI}'"
	} else if (!projectSettingsFileG.equals("")) {
		projectSettingsFile = projectSettingsFileG
		mavenCommand += " --settings '${projectSettingsFileG}'"
	}
	if(globalSettingsFile.startsWith("http")){
		downloadSettingsFromUrl(projectSettingsFile)
	}


	/* 4. pomPath */
	String pomPath
	String pomPathI = getStrValue(inputBuildParameters.pomPath)
	String pomPathG = getStrValue(globalStepConfigs.pomPath)

	if(pomPathI.equals("") && pomPathG.equals("")) {

	} else if (!pomPathI.equals("")) {
		pomPath = pomPathI
		mavenCommand += " --file '${pomPathI}'"
	} else if (!pomPathG.equals("")) {
		pomPath = pomPathG
		mavenCommand += " --file '${pomPathG}'"
	}


	/* 5. m2Path */
	String m2Path
	String m2PathI = getStrValue(inputBuildParameters.m2Path)
	String m2PathG = getStrValue(globalStepConfigs.m2Path)

	if(m2PathI.equals("") && m2PathG.equals("")) {

	} else if (!m2PathI.equals("")) {
		m2Path = m2PathI
		mavenCommand += " -Dmaven.repo.local='${m2PathI}'"
	} else if (!m2PathG.equals("")) {
		m2Path = m2PathG
		mavenCommand += " -Dmaven.repo.local='${m2PathG}'"
	}

	return mavenCommand
}

private loadGlobalStepConfigs(){
	Map globalStepConfigs  = readYaml text: libraryResource('global_configurations.yml')
	echo "globalStepConfigs: ${globalStepConfigs} "
	return globalStepConfigs;
}

private downloadSettingsFromUrl(String url){
	def settings = httpRequest url
	writeFile file: 'settings.xml', text: settings.getContent()
}

private getStrValue(String str) {
	String newStr = "";
	if(!(str?.trim().equals(""))) {
		newStr = str;
	}
	return newStr
}

