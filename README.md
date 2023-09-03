# real-world-clojure-api

**Author**: Andrey Fadeev

## Introduction

This is a Clojure project titled "real-world-clojure-api" by Andrey Fadeev. It has dependencies on various libraries such as `aero`, `pedestal`, `slf4j`, and `component`. This README provides instructions on how to set up your environment and run the project.

## Video Series

For a comprehensive walkthrough and detailed explanations of the project, you can watch the video series available on Andrey Fadeev's YouTube channel:

[Real-World Clojure API Video Series](https://youtube.com/playlist?list=PLRGAFpvDgm2ylbXYIjvu3kI426zAP_Lqc&si=00MjPUe_aL0h8Nbi)

[Andrey Fadeev's YouTube Channel](https://www.youtube.com/@andrey.fadeev)

## Prerequisites

1. **Java**: Ensure you have Java installed. Clojure runs on the Java Virtual Machine (JVM), so you'll need Java to run Clojure code.

2. **Clojure CLI (`clj`)**: This project uses the Clojure CLI tool (`clj`) for dependency management and running Clojure scripts.

## Installation

### Installing Clojure CLI (`clj`)

If you haven't installed the Clojure CLI (`clj`) yet, follow these steps:

#### For macOS:

Using Homebrew:

```bash
brew install clojure/tools/clojure
```

#### For Linux:

Download the script:

```bash
curl -O https://download.clojure.org/install/linux-install-1.10.3.967.sh
chmod +x linux-install-1.10.3.967.sh
sudo ./linux-install-1.10.3.967.sh
```

#### For Windows:

- **Using Windows Subsystem for Linux (WSL)**: It's recommended to use the [Windows Subsystem for Linux (WSL)](https://docs.microsoft.com/en-us/windows/wsl/install) and then follow the Linux instructions.

- **Using Git Bash**: If you have Git Bash installed, you can use it to run the Linux installation commands. Open Git Bash and follow the Linux instructions above.

### Clone the Project

If you haven't already, clone this project to your local machine:

```bash
git clone https://github.com/andfadeev/real-world-clojure-api.git
cd real-world-clojure-api
```

## Running the Project

1. Navigate to the project directory:

```bash
cd real-world-clojure-api
```

2. Start the project in development mode:

```bash
clj -A:dev
```

This command uses the `:dev` alias specified in the project configuration to start the project in development mode. It will load the `dev` namespace and execute the required functions.

## Dependencies

This project uses the following dependencies:

- `aero`: Configuration library.
- `pedestal`: Web application framework.
- `slf4j`: Simple Logging Facade for Java.
- `component`: Manage the lifecycle of stateful objects in Clojure.

For detailed versions, refer to the project configuration.

## Conclusion

You should now have everything set up and running. Explore the code, make changes, and enjoy your Clojure development experience! If you encounter any issues, please refer to the respective library's documentation, watch the video series on Andrey Fadeev's YouTube channel for guidance, or raise an issue in this repository.