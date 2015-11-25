


# Flow of http routes

1- `resources/routes.txt` links urls to keymap
2- `freecoin.core/handlers` links keymap to handlers
3- `src/freecoin/handlers/`   exec handlers and fills views
4- `src/freecoin/views/`        web ready formats for styled views

Most of the computation happens in handlers

Most of the presentation happens in views

