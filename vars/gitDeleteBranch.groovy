/**
 * Delete a remote Git branch over HTTPS using Jenkins credentials (e.g. GitHub App creds).
 *
 * Required:
 * - remoteUrl     — repository URL (e.g. https://github.com/org/repo.git)
 * - branch        — remote branch name to delete
 * - credentialsId — Jenkins username/password credential id
 */
def call(Map args) {
    if (!args.remoteUrl?.toString()?.trim()) {
        error('gitDeleteBranch: remoteUrl is required')
    }
    if (!args.branch?.toString()?.trim()) {
        error('gitDeleteBranch: branch is required')
    }
    if (!args.credentialsId?.toString()?.trim()) {
        error('gitDeleteBranch: credentialsId is required')
    }

    def remoteUrl = args.remoteUrl.toString().trim()
    def branch = args.branch.toString().trim()
    def credentialsId = args.credentialsId.toString().trim()

    if (!remoteUrl.startsWith('https://')) {
        error('gitDeleteBranch: remoteUrl must be an https:// URL')
    }

    def repoPath = remoteUrl.replace('https://', '')

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'GH_USER',
            passwordVariable: 'GH_TOKEN'
        )
    ]) {
        def repoUrl = "https://${env.GH_USER}:${env.GH_TOKEN}@${repoPath}"
        sh """
            set +e
            git push '${repoUrl}' --delete '${branch}' || true
        """.stripIndent().trim()
    }
}
