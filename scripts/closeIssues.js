import { Octokit } from "@octokit/rest";

const octokit = new Octokit({
    auth: process.env.GITHUB_TOKEN,
});

async function closeIssues() {
    try {
        console.log('Fetching issues...');
        
        // 1. Get all open issues (about 300 → 3 API calls with per_page=100)
        const openIssues = await octokit.paginate(octokit.rest.issues.listForRepo, {
            owner: 'triplea-game',
            repo: 'triplea',
            state: 'open',
            per_page: 100,
        });
        console.log(`Fetched ${openIssues.length} issues in total.`);

        // 2. Get recently (=updated in the last year) closed issues (not more than 300 expected → 3 API calls max)
        const sinceDate = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000).toISOString();
        
        const recentlyClosedIssues = [];
        for await (const response of octokit.paginate.iterator(
          octokit.rest.issues.listForRepo,
          {
          owner: "triplea-game",
          repo: "triplea",
          state: "closed",
          sort: "updated",
          direction: "desc",
          since: sinceDate,
          per_page: 100,
          }
        )) { 
          for (const issue of response.data) {
            // extra check on closed_at as we focus strictly on closure date (later comments affect updated again)
            if (
              issue.closed_at &&
              new Date(issue.closed_at) >= new Date(sinceDate) &&
              !issue.labels.some(l => l.name === "auto-close")
            ) {
              recentlyClosedIssues.push(issue);
            }
          }
        }
        console.log(`Fetched ${recentlyClosedIssues.length} recently closed issues in total.`);
        
        for (const issue of openIssues) {
            // Filter out those with label 'avoidAutoClose'
            if (issue.labels.some(l => l.name === "avoidAutoClose")) {
                continue; // label exists, so no close-check required
            }
            
            console.log(`Checking issue #${issue.number}...`);
            let closeNeeded = false;
            let closeMessage = '';
            // Add label '2.5' (if missing)
            const checkPrefix2_5 = (issue) => issue.title.startsWith('2.5.');
            if (await checkAndLabelIssue(issue, '2.5', checkPrefix2_5)) {
                closeNeeded = true;
                closeMessage = `Closing as issue from version 2.5 are assumed to be fixed in newest release.`
            } 
                
            if (!closeNeeded) {
                // Check for earliest open issue (then it is a duplicate))
                const findEarliestDuplicate = (issue, issues) => {
                  const duplicates = issues.filter(
                    (other) =>
                      other.title === issue.title &&
                      new Date(other.created_at) < new Date(issue.created_at) &&
                      other.number !== issue.number
                  );
                
                  if (duplicates.length === 0) return null;
                
                  return duplicates.reduce((earliest, current) =>
                    new Date(current.created_at) < new Date(earliest.created_at)
                      ? current
                      : earliest
                  );
                };
                const earliestOpenDuplicate = findEarliestDuplicate(issue, openIssues);
                if (earliestOpenDuplicate) {
                    closeNeeded = true;    
                    closeMessage = `Closing as duplicate (indicated by same title) of #${earliestOpenDuplicate.number}.`;
                } else {
                    const earliestRecentlyClosedDuplicate = findEarliestDuplicate(issue, recentlyClosedIssues);
                    if (earliestRecentlyClosedDuplicate) {
                        closeNeeded = true;    
                        closeMessage = `Closing as duplicate (indicated by same title) of #${earliestRecentlyClosedDuplicate.number} is already closed.`;
                    }
                }
            }

            if (closeNeeded) {
                // Add label 'auto-close' (if missing)
                await checkAndLabelIssue(issue, 'auto-close', (issue) => true);

                // Add last comment before closing (cannot be combined in one API call)
                if (closeMessage) {
                    await octokit.rest.issues.createComment({
                      owner: 'triplea-game',
                      repo: 'triplea',
                      issue_number: issue.number,
                      body: closeMessage,
                    });
                }
                
                // Add close issue
                await octokit.rest.issues.update({
                    owner: 'triplea-game',
                    repo: 'triplea',
                    issue_number: issue.number,
                    state: 'closed',
                    labels: issue.labels,  // ensure the labels are updated
                });
                console.log(`Closed issue #${issue.number}${closeMessage ? " (" + closeMessage + ")" : ""}.`);
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
