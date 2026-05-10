/**
 * Close a GitHub pull request.
 *
 * Required:
 * - prNumber        — pull request number (Integer or String)
 * - githubRepo      — HTTPS clone URL (e.g. https://github.com/org/repo.git)
 * - credentialsId   — Jenkins username/password; token used as Bearer for api.github.com
 */
def call(Map args) {
    if (!args.containsKey('prNumber') || args.prNumber == null) {
        error('closePullRequest: prNumber is required')
    }
    if (!args.githubRepo?.toString()?.trim()) {
        error('closePullRequest: githubRepo is required')
    }
    if (!args.credentialsId?.toString()?.trim()) {
        error('closePullRequest: credentialsId is required')
    }

    def prNumber = args.prNumber.toString().trim()
    def githubRepo = args.githubRepo.toString().trim()
    def credentialsId = args.credentialsId.toString().trim()

    if (!githubRepo.startsWith('https://')) {
        error('closePullRequest: githubRepo must be an https:// URL')
    }

    def repoNoGit = githubRepo.replaceAll(/\.git\/?$/, '')
    def slug = repoNoGit.replace('https://github.com/', '')
    def slugParts = slug.split('/')
    if (slugParts.length < 2) {
        error("closePullRequest: cannot parse owner/repo from githubRepo: ${githubRepo}")
    }
    def ghOwner = slugParts[0]
    def ghRepo = slugParts[1]

    writeFile(file: 'gitops-pr-close-payload.json', text: '{"state":"closed"}')

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'GH_USER',
            passwordVariable: 'GH_TOKEN'
        )
    ]) {
        def httpCode = sh(
            script: """
                set -eu
                curl -sS -o gitops-pr-close-response.json -w '%{http_code}' \\
                    -X PATCH \\
                    -H 'Accept: application/vnd.github+json' \\
                    -H "Authorization: Bearer \${GH_TOKEN}" \\
                    -H 'X-GitHub-Api-Version: 2022-11-28' \\
                    "https://api.github.com/repos/${ghOwner}/${ghRepo}/pulls/${prNumber}" \\
                    -d @gitops-pr-close-payload.json
            """.stripIndent().trim(),
            returnStdout: true
        ).trim()

        if (httpCode != '200') {
            def body = readFile('gitops-pr-close-response.json')
            echo "closePullRequest: GitHub API HTTP ${httpCode}: ${body}"
            error("closePullRequest: failed to close PR #${prNumber} (HTTP ${httpCode})")
        }
        echo "closePullRequest: closed PR #${prNumber} on ${ghOwner}/${ghRepo}"
    }
}
