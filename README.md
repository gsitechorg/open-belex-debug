# belex-dbg

```
$ mamba env create --force -f environment.yml
$ conda activate belex-dbg
$ pip install -e .
$ npm install

# for development or production:
$ ./node_modules/.bin/sass \
      src/styles/screen.scss \
      resources/public/css/compiled/screen.css

# for interactive development:
# ./node_modules/.bin/sass --watch \
#     src/styles/screen.scss \
#     resources/public/css/compiled/screen.css

$ npx shadow-cljs compile electron browser  # for development
# npx shadow-cljs release electron browser  # for production
# npx shadow-cljs watch electron browser  # for interactive development
```
