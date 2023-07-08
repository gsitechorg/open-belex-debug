# belex-dbg

## Initializing the Environment

Be sure to follow the instructions to set up our
[`conda_channel`](https://gsitech.gitlab.io/conda_channel.html), then execute
the following:

```shell
# clone belex-debug to /path/to/belex-debug
git clone git@bitbucket.org:gsitech/belex-debug.git
cd belex-debug
mamba env create --force -f environment.yml
conda activate belex-dbg
pip install -e .
npm install
```

Once the environment has been initialized, you need to build the CSS and Web
App, per the following instructions.

### Using the `development` version of `belex`

To use the latest development version of `belex`, git-clone it and then install
it with `pip install -e`:

```shell
# clone belex to /path/to/belex
git clone --branch develop git@bitbucket.org:gsitech/belex.git
cd /path/to/belex-debug
conda activate belex-dbg  # if not already active
pip install -e /path/to/belex
```

## Compiling Assets

### Compiling the CSS

For standard development or production environments, execute the following
command:

```shell
./node_modules/.bin/sass \
    src/styles/screen.scss \
    resources/public/css/compiled/screen.css
```

For an interactive development environment, execute the command with `--watch`:

```shell
./node_modules/.bin/sass --watch \
    src/styles/screen.scss \
    resources/public/css/compiled/screen.css
```

### Compiling the Web App

For a standard development build, execute the following (this will need to be
run after each change made to the app):

```shell
npx shadow-cljs compile electron browser
```

For an interactive development environment, execute the following (this will
automatically update the app when the code is changed):

```shell
npx shadow-cljs watch electron browser
```

For a production build, execute the following:

```shell
npx shadow-cljs release electron browser
```

## Debugging a Belex App

Once the debugger has been initialized and compiled, a Belex app located at
`/path/to/belex-app` with a main script `main.py` may be debugged as follows:

```shell
cd /path/to/belex-app
# {!} IMPORTANT: initialize the belex-app environment with conda, venv, or
# however it should be done
pip install -e /path/to/belex-debug
belex-dbg main.py
```

This will launch an Electron (Chromium) window that hosts the debugger. If you
want to pass arguments to `main.py`, separate them from the arguments to
`belex-dbg` with `--`, as follows:

```shell
belex-dbg main.py -- --some-flag --flag-with-value value arg1 arg2
```

If you do not separate the arguments with `--`, they will be assumed arguments
for `belex-dbg`.

If you want to debug `belex-dbg`, you may pass it the `--no-gui` flag which will
launch the web server but not the Electron window.

```shell
belex-dbg --no-gui main.py -- --some-flag --flag-with-value value arg1 arg2
```

You may then navigate to the URL for the server and debug the debugger with your
browser's development tools. At the moment, only Google Chrome / Chromium is
supported. The app may work in other browsers, but only Chrome is fully
supported.
