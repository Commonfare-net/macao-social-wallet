Confirmation API notes
===============

Creating a wallet:
----------

"/signup"

POSTS to
"/signup-requests" -- creates a signup-request

- Triggers action to take in order to process the 'signup-request'
  - email, web form, third-party service

- Recommend starting with simple web-form:
  redirect to:
  GET /signup-requests/request-id

  - If request has not expired, then shows a confirmation form with captcha

  - If request has expired... indicates this with a message.

If request is confirmed, then
"POST /wallets" with JSON / form data to create wallet, along with token to verify confirmation

If confirmation is cancelled, then
"PUT /signup-confirmations/confirmation-id" to cancel the confirmation

Sending freecoins:
--------------

"/send"  (GET: form for creating a send transaction)

"/send-confirmations"  (POST to create a 'confirmation' to represent the unconfirmed send transaction)

... acting on the confirmation --- configurable; but recommend starting with a simple web-based confirmation

confirmed; post to
"/transactions"

cancelled;
PUT to /send-confirmations --- to cancel the confirmation
