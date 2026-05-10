/**
 * Create a GitHub pull request via the REST API.
 *
 * Required:
 * - prMessage       — PR title
 * - githubRepo      — HTTPS clone URL (e.g. https://github.com/org/repo.git)
 * - headBranch      — head branch name (e.g. env.GITOPS_BRANCH)
 * - baseBranch      — base branch (e.g. main)
 * - credentialsId   — Jenkins username/password; token used as Bearer for api.github.com
 *
 * Returns the pull request number (Integer) on HTTP 201, or when HTTP 422 means a PR
 * already exists (resolved via list pulls by head). Returns null if the PR was not opened
 * and could not be resolved (caller should fail the pipeline).
 * Input validation failures still call error().
 */
def call(Map args) {
    ['prMessage', 'githubRepo', 'headBranch', 'baseBranch', 'credentialsId'].each { key ->
        if (!args.containsKey(key) || !args[key]?.toString()?.trim()) {
            error("createPullRequest: missing or empty required parameter '${key}'")
        }
    }

    def prMessage = args.prMessage.toString().trim()
    def githubRepo = args.githubRepo.toString().trim()
    def headBranch = args.headBranch.toString().trim()
    def baseBranch = args.baseBranch.toString().trim()
    def credentialsId = args.credentialsId.toString().trim()

    if (!githubRepo.startsWith('https://')) {
        error('createPullRequest: githubRepo must be an https:// URL')
    }

    def repoNoGit = githubRepo.replaceAll(/\.git\/?$/, '')
    def slug = repoNoGit.replace('https://github.com/', '')
    def slugParts = slug.split('/')
    if (slugParts.length < 2) {
        error("createPullRequest: cannot parse owner/repo from githubRepo: ${githubRepo}")
    }
    def ghOwner = slugParts[0]
    def ghRepo = slugParts[1]

    def buildNum = env.BUILD_NUMBER ?: ''
    def buildUrl = env.BUILD_URL ?: ''
    def prPayload = new groovy.json.JsonBuilder([
        title: prMessage,
        head : headBranch,
        base : baseBranch,
        body : "Automated GitOps PR from Jenkins build ${buildNum}\n${buildUrl}",
    ]).toString()

    writeFile(file: 'gitops-pr-payload.json', text: prPayload)

    def prNumber = null
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
                curl -sS -o gitops-pr-response.json -w '%{http_code}' \\
                    -X POST \\
                    -H 'Accept: application/vnd.github+json' \\
                    -H "Authorization: Bearer \${GH_TOKEN}" \\
                    -H 'X-GitHub-Api-Version: 2022-11-28' \\
                    "https://api.github.com/repos/${ghOwner}/${ghRepo}/pulls" \\
                    -d @gitops-pr-payload.json
            """.stripIndent().trim(),
            returnStdout: true
        ).trim()

        if (httpCode == '201') {
            def body = readFile('gitops-pr-response.json')
            def parsed = new groovy.json.JsonSlurper().parseText(body)
            prNumber = parsed.number as Integer
            echo "createPullRequest: GitOps PR #${prNumber} created (${httpCode}): ${prMessage}"
        } else {
            def resp = readFile('gitops-pr-response.json')
            echo "createPullRequest: GitHub PR API HTTP ${httpCode}: ${resp}"
            if (httpCode == '422' && resp.toLowerCase().contains('already exists')) {
                def headParam = java.net.URLEncoder.encode("${ghOwner}:${headBranch}", 'UTF-8')
                def listCode = sh(
                    script: """
                        set -eu
                        curl -sS -o gitops-pr-list.json -w '%{http_code}' \\
                            -H 'Accept: application/vnd.github+json' \\
                            -H "Authorization: Bearer \${GH_TOKEN}" \\
                            -H 'X-GitHub-Api-Version: 2022-11-28' \\
                            "https://api.github.com/repos/${ghOwner}/${ghRepo}/pulls?head=${headParam}&state=open"
                    """.stripIndent().trim(),
                    returnStdout: true
                ).trim()
                if (listCode == '200') {
                    def listBody = readFile('gitops-pr-list.json')
                    def arr = new groovy.json.JsonSlurper().parseText(listBody) as List
                    if (arr && !arr.isEmpty()) {
                        prNumber = arr[0].number as Integer
                        echo "createPullRequest: existing PR #${prNumber} for head ${headBranch}"
                    }
                } else {
                    echo "createPullRequest: list pulls HTTP ${listCode}"
                }
            }
        }
    }
    return prNumber
}
