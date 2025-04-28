import { Octokit } from "@octokit/rest";

const octokit = new Octokit({
    auth: process.env.GITHUB_TOKEN,
});

async function closeIssues() {
    try {
        console.log('Fetching issues...');
        const issues = await octokit.paginate(octokit.rest.issues.listForRepo, {
            repo: 'triplea',
            state: 'open',
        });

        console.log(`Fetched ${issues.length} issues.`);
        const issuesToClose = issues.filter(issue => issue.title.startsWith('2.5.') );
        console.log(`Found ${issuesToClose.length} issues to close.`);

        for (const issue of issuesToClose) {
            console.log(`Closing issue #${issue.number}...`);
          /*
            await octokit.rest.issues.update({
                repo: 'triplea',
                issue_number: issue.number,
                state: 'closed',
            });
            */
            console.log(`Closed issue #${issue.number}.`);
        }
    } catch (error) {
        console.error(error);
    }
}

closeIssues();
