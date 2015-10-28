#  ____________________
# < created by ansible >
#  --------------------
#         \   ^__^
#          \  (oo)\_______
#             (__)\       )\/\
#               U ||----w |
#                 ||     ||
#  

# set PATH so it includes user's private bin if it exists
if [ -d "$HOME/bin" ] ; then
    PATH="$HOME/bin:$PATH"
fi
 
alias ll="ls -la"
alias lh="ls -ltrh"

#EOF
