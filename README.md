

```edn
{:deps
 {aero/aero {:mvn/version "1.1.6"}
  
  
  io.pedestal/pedestal.service {:mvn/version "0.6.0"}
  io.pedestal/pedestal.route {:mvn/version "0.6.0"}
  io.pedestal/pedestal.jetty {:mvn/version "0.6.0"}
  org.slf4j/slf4j-simple {:mvn/version "2.0.7"}
  
  
  com.stuartsierra/component {:mvn/version "1.1.0"}
  com.stuartsierra/component.repl {:mvn/version "0.2.0"}}

 :paths ["src" "resources" "dev"]}
```


```edn
 :aliases {:dev {:main-opts ["-e" "(require,'dev)"
                             "-e" "(in-ns,'dev)"]}}
```