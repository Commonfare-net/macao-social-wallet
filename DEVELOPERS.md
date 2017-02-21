


# Flow of http routes

1- `src/freecoin/routes.clj` bidi route binidngs
2- `freecoin.core/handlers` links keymap to handlers
3- `src/freecoin/handlers/`   exec handlers and fills views
4- `src/freecoin/views/`        web ready formats for styled views

Most of the computation happens in handlers

Most of the presentation happens in views

