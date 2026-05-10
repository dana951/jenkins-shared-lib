/**
 * Verify k8s Deployment image.
 *
 * Runs:
 *   kubectl get deployment <deployment-name> -n <namespace> \
 *     -o jsonpath='{.spec.template.spec.containers[0].image}'
 *
 * Required:
 * - deploymentName — Kubernetes Deployment metadata.name
 * - namespace      — namespace of the Deployment
 * - expectedImage  — full image ref to match exactly (e.g. devuser103/podinfo:0.1.2-amd)
 */
def call(Map args) {
    ['deploymentName', 'namespace', 'expectedImage'].each { key ->
        if (!args.containsKey(key) || !args[key]?.toString()?.trim()) {
            error("verifyK8sDeploymentImageTag: missing required parameter '${key}'")
        }
    }

    def deploymentName = args.deploymentName.toString().trim()
    def namespace = args.namespace.toString().trim()
    def expectedImage = args.expectedImage.toString().trim()

    def image = sh(
        script: "kubectl get deployment '${deploymentName}' -n '${namespace}' -o jsonpath='{.spec.template.spec.containers[0].image}'",
        returnStdout: true
    ).trim()

    echo "verifyK8sDeploymentImageTag: deployment '${deploymentName}' has image: '${image}'"
    if (image != expectedImage) {
        error("verifyK8sDeploymentImageTag: got '${image}', expected '${expectedImage}' (deployment '${deploymentName}' ns '${namespace}')")
    }
    echo '✅ verifyK8sDeploymentImageTag: image matches'
}
