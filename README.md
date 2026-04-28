# jenkins-shared-lib

Shared Jenkins Pipeline Library that provides reusable CI/CD steps.

This repository is part of the larger [`eks-gitops-platform`](https://github.com/dana951/eks-gitops-platform) portfolio.  
Its responsibility is to centralize common pipeline logic so Jenkinsfiles stay small and consistent.

## What This Repo Does

- Provides reusable pipeline steps under `vars/` for Jenkins Declarative pipelines.

## Where It Fits in the Platform

This shared library is consumed by Jenkins pipelines in [`app-source`](https://github.com/dana951/app-source.git).


## Repository Layout

```text
jenkins-shared-lib/
└── vars/           # pipeline steps
```

## Related Repositories

- Platform overview: [`eks-gitops-platform`](https://github.com/dana951/eks-gitops-platform)
- Infrastructure (EKS, Jenkins, Argo CD): [`infra-aws`](https://github.com/dana951/infra-aws)
- Application source and Jenkinsfile: [`app-source`](https://github.com/dana951/app-source.git)
- GitOps workload definitions: [`gitops-manifests`](https://github.com/dana951/gitops-manifests.git)
- Argo CD bootstrap repo: [`argocd-apps`](https://github.com/dana951/argocd-apps.git)
- Test automation repository: [`tests-repo`](https://github.com/dana951/tests-repo.git)

## License

See `LICENSE` in this repository.
