export PATH := $(PATH):restdown/bin

all: api

# dot: dot/freecoin_nxt.dot dot/freecoin_secretshare.dot
# 	cd .. && lein gossip && cd -

# png: freecoin_nxt.png freecoin_secretshare.png
# 	dot -Tpng $<

api:
	restdown API.md -m .
