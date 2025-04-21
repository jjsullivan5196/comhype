## Quickstart/demo

Install JDK 11+ and Clojure https://clojure.org/guides/install_clojure

Use your favorite IDE or run `clojure`, then `(load "comhype/core")` to start the app. It will be at http://localhost:5055

## Experiments with serverside view models

I'm trying to create something akin to Elm but with 99% of the
application model running on the server, including view models ala
LiveView.

Ideally this would be done with the fewest dependencies as reasonable,
so I've made a proof-of-concept design only using a few web
primitives. As it is now, it roughly works by these steps:

- Define your application model/state and a function to generate an HTML view of it
  - The client can invoke the command/`cmd` function in response to browser events to send commands to the server
- Create a "command" function that defines state transitions for your app
- Use both to construct the example ring handler, it will route commands and view changes amongst all connected clients

Essentially the flow is `(view-function model) -> HTML view model -> generates commands -> update model`

The application server coordinates everything in the application
domain, only reading commands from clients. To update clients, the
server will send script snippets to update the view/view model on the
client side.

## Inspiration from

- https://github.com/rabbibotton/clog
- https://github.com/phoenixframework/phoenix_live_view
- https://elm-lang.org/
- https://htmx.org/
- https://leanrada.com/htmz/