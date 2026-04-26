// SHARED LIBRARY: Docker Utilities
//
// Verifying an image exists in ECR before deploying.
// Every pipeline that deploys a Docker image needs this check.
// HOW TO USE IN A JENKINSFILE:
//   @Library('podinfo-shared-lib') _
//   def imageUri = dockerUtils.verifyImageInECR('123456789.dkr.ecr.us-east-1.amazonaws.com/podinfo', 'abc1234')

/**
 * Verify that a Docker image tag exists in ECR before attempting to deploy it.
 * Fails the pipeline immediately if the image is not found.
 *
 * @param ecrRegistry  Full ECR registry URL (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com)
 * @param repository   ECR repository name (e.g. podinfo)
 * @param imageTag     Image tag to verify (e.g. abc1234 or v0.2.0)
 * @param awsRegion    AWS region (e.g. us-east-1)
 */
def verifyImageInECR(String ecrRegistry, String repository, String imageTag, String awsRegion) {
    echo "Verifying image exists in ECR: ${ecrRegistry}/${repository}:${imageTag}"

    def exists = sh(
        script: """
            aws ecr describe-images \
                --registry-id ${ecrRegistry.split('\\.')[0]} \
                --repository-name ${repository} \
                --image-ids imageTag=${imageTag} \
                --region ${awsRegion} \
                --query 'imageDetails[0].imageTags' \
                --output text 2>/dev/null
        """,
        returnStatus: true
    ) == 0

    if (!exists) {
        error("Image ${ecrRegistry}/${repository}:${imageTag} not found in ECR. " +
              "Did the GHA build job succeed? Aborting deployment.")
    }

    echo "Image verified in ECR: ${ecrRegistry}/${repository}:${imageTag}"
    return "${ecrRegistry}/${repository}:${imageTag}"
}