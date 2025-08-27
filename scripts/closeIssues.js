import { Octokit } from "@octokit/rest";

const octokit = new Octokit({
    auth: process.env.GITHUB_TOKEN,
});

async function closeIssues() {
    try {
        console.log('Fetching issues...');
        const openIssues = await octokit.paginate(octokit.rest.issues.listForRepo, {
            owner: 'triplea-game',
            repo: 'triplea',
            state: 'open',
        });
        console.log(`Fetched ${openIssues.length} open issues.`);

        for (const issue of openIssues) {
            // Filter out those with label 'avoidAutoClose'
            if (issue.labels.some(l => l.name === "avoidAutoClose")) {
                continue; // label exists, so no check required
            }
            
            console.log(`Checking issue #${issue.number}...`);
            // Add label '2.5' (if missing)
            const checkPrefix2_5 = (issue) => issue.title.startsWith('2.5.');
            let closeNeeded = await checkAndLabelIssue(issue, '2.5', checkPrefix2_5);

            // Check for earlier open issue (then it is a duplicate))
            const duplicates = openIssues.filter( other =>
              other.title === issue.title &&
              new Date(other.created_at) < new Date(issue.created_at) &&
              other.number !== issue.number
            );
            if (duplicates.length > 0) {
                closeNeeded = true;
                const earliest = duplicates.reduce((prev, curr) =>
                   new Date(curr.created_at) < new Date(prev.created_at) ? curr : prev
                );
                console.log(`Duplicate (indicated by same title) of #${earliest.number}.`);
                await octokit.rest.issues.createComment({
                  owner: 'triplea-game',
                  repo: 'triplea',
                  issue_number: issue.number,
                  body: `Closing as duplicate (indicated by same title) of #${earliest.number}.`,
                });
            }

            if (closeNeeded) {
                console.log(`Issue #${issue.number} should be closed.`);
                // Add label 'auto-close' (if missing)
                await checkAndLabelIssue(issue, 'auto-close', (issue) => true);
                
                // Add close issue
                await octokit.rest.issues.update({
                    owner: 'triplea-game',
                    repo: 'triplea',
                    issue_number: issue.number,
                    state: 'closed',
                    labels: issue.labels,  // ensure the labels are updated
                });
                console.log(`Closed issue #${issue.number}.`);
            }  
            else {
                console.log(`Nothing to be done for #${issue.number}.`);
            }
        }
    } catch (error) {
        console.error(error);
    }
}
    
/**
 * Check if the issue title starts with a prefix and apply a label if needed.
 * @param {Object} issue - GitHub issue object.
 * @param {string} label - Label to apply.
 * @param {Function} checkFn - A function that returns true if issue matches condition.
 * @returns {Promise<boolean>} - True if issue matches the prefix, false otherwise.
 */
async function checkAndLabelIssue(issue, label, checkFn) {
  const passed = checkFn(issue);
  if (!passed) return false;

  const hasLabel = issue.labels.some(l => l.name === label);
  if (!hasLabel) {
    console.log(`Adding label "${label}" to issue #${issue.number}`);
    issue.labels.push(label);
  }
  return true;
}

closeIssues();
