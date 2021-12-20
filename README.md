## Assumptions

- Random prices on each request is to what the solution should be optimized. Meaning that I will not assume "its just a dummy data", but its how 3rd party client works. This assumption has impact on caching choice
- Instance kinds do not change

## Design decisions

- Going over the quota is extremely problematic due few reasons:
  - Instance prices cannot be cached, because prices fluctuate immediately due to the nature of it being random
  - There is no way to increase quota, meaning if quota is reached - user has to be informed that he no longer can make requests
- Instance kinds can be cached, because they do not change
- Scalafmt change to maxColumn = 80, because it doesn't fit on the laptop I am temporary working on
- 
