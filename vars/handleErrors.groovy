
def call(Map parameters = [:], body) {

	def stepParameters = parameters.stepParameters //mandatory
	def stepName = parameters.stepName //mandatory
	def echoDetails = parameters.get('echoDetails', true)

	try {

		if (stepParameters == null && stepName == null)
			error "step handlePipelineStepErrors requires following mandatory parameters: stepParameters, stepName"

		body()

	} catch (Throwable err) {

		if (echoDetails)
			echo """ERROR OCCURED IN LIBRARY STEP: ${stepName}, FOLLOWING PARAMETERS WERE AVAILABLE TO THIS STEP: ${stepParameters} and ERROR WAS: ${err} """
		throw err

	} finally {

	}
}
