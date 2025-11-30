# Contributing to MeBudget

First off, thanks for taking the time to contribute! 🎉

The following is a set of guidelines for contributing to MeBudget. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

## Code of Conduct

This project and everyone participating in it is governed by the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/1/4/code-of-conduct). By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for MeBudget. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

- **Use a clear and descriptive title** for the issue to identify the problem.
- **Describe the exact steps to reproduce the problem** in as many details as possible.
- **Provide specific examples** to demonstrate the steps.
- **Describe the behavior you observed** after following the steps and point out what exactly is the problem with that behavior.
- **Explain which behavior you expected to see instead** and why.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for MeBudget, including completely new features and minor improvements to existing functionality.

- **Use a clear and descriptive title** for the issue to identify the suggestion.
- **Provide a step-by-step description of the suggested enhancement** in as many details as possible.
- **Explain why this enhancement would be useful** to most MeBudget users.

### Pull Requests

1.  **Fork the repo** and create your branch from `main`.
2.  **Clone the project** to your own machine.
3.  **Commit changes** to your own branch.
4.  **Push** your work back up to your fork.
5.  **Submit a Pull Request** so that we can review your changes.

NOTE: Be sure to merge the latest from "upstream" before making a pull request!

## Development Setup

1.  **Clone the repository**
    ```bash
    git clone https://github.com/yourusername/mebudget.git
    cd mebudget
    ```

2.  **Install dependencies**
    ```bash
    npm install
    ```

3.  **Set up Environment Variables**
    Copy `.env.example` to `.env` and fill in your Supabase credentials.

4.  **Run the app**
    ```bash
    npm run dev
    ```

## Styleguides

### Git Commit Messages

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

### JavaScript Styleguide

- We use **ESLint** to enforce code style.
- Run `npm run lint` to check for style issues.
- Prefer `const` over `let`. Avoid `var`.
- Use functional components and Hooks for React.

Thank you for contributing! 🚀
