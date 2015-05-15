---
title: Freecoin API
---

# Introduction

This is work in progress

# Wallet

## GET /wallet

Open the wallet

If no cookie set, ask to create or recover

## GET /wallet/create

If a valid cookie is already set, then return "already created"

Proceed asking nick and email, or offer link to recovery procedure

## POST /wallet/create

Check nick existance and send email confirmation

#### example request

    curl http://nxt.dyne.org/wallet/create \
        -F "nick=luther" \
        -F "email=luther@dyne.org"

#### example response

    {
        "success": "Check your email to confirm creation."
        "email": "luther@dyne.org"
    }

## GET /wallet/create/`confirmation`

Check if email `confirmation` code is correct (and the cookie is set?), then proceed creating the wallet

## GET /wallet/recover

Ask for the nick and/or email and the secret pins to recover

## POST /wallet/recover

If POST fields :nick or :email plus both :ah and :al pins are present, then restore a wallet

#### example request

    curl https://fxc.dyne.org/wallet/recover \
        -F "email=luther@dyne.org" \
        -F "ah=ASE33DEF5" \
        -F "al=GT674GOP4"

#### example response

Will setup session cookies on device's browser and return success.

    {
        "success": "Access to wallet succesfully recovered."
    }

# Sending

Sending among participants is optional and depends from the monetary system setting by the issuing organization. If allowed the send API will be available for a "free market" among participants, making them able to transfer amounts among themselves.

## GET /send

Ask for an amount to send and a destinatary, then POST /send

## GET /send/`participant`

Ask for an amount to send to `participant` then POST /send

## GET /send/`participant`/`amount`

Ask confirmation to send `amount` to `participant`, then POST /send

## POST /send

Do send `amount` to `participant`

#### example request

    curl https://fxc.dyne.org/send \
        -F "amount=100" \
        -F "participant=luther"

#### example response

    {
        "success": "Succesfully sent 100 to luther"
        "amount": 100
        "recipient": "luther"
    }

# Stashes

A stash is a pre-defined amount that can be transferred off-line using a secret code or image (qrcode)

## GET /stash/create/:amount

Create a signed stash of :amount ready to be given

## GET /stash/claim

Ask for the secret code of a stash to claim

## GET /stash/claim/:stash-id

Claim a stash of coins by url (direct link from qrcode)



# Clearing house

Clearing house must be interfaced with democratic decision making tools

Objective8, YourPriorities, DemocracyOS, Mutual_Credit

## TODO



# Vendor relations

Vendor value transactions must be customized ad-hoc for pilots: transport companies, tax departments, service providers.

## GET /vendor/send/:participant/:amount/:vendor

Send :amount to :vendor in exchange of service credit to :participant


# Encryption scheme

SecretShare authentication in Freecoin is based on the Shamir Secret Sharing Scheme (SSSS).

We envision a scenario where we have three actors:

- Organization (minting and administering the currency)
- Participant (single individuals using the currency)
- Vendor (third-party providing value for the currency)

We want a situation in which one of these actors alone is not able to access the wallet, but at least the consent of two actors is necessary, where the most common case will be:

- Organization & Participant (to view funds, send and receive)
- Organization & Vendor (to redeem currency)

The last case, Participant & Vendor, will likely not occur in normal situations, but is a warranty that the funds will exist even if the Organization disappears.

## Implementation

This implementation is called "FXC protocol" and this is version 1.

The FXC passphrase is created by two random sequences in the form `FXC1_random_FXC_random_0`

The two random parts are then split in 9 slices with quorum 5 using SSSS, the NXT passphrase is deleted and only the SSSS slices can be used to retrieve it.

Slices form a 2x9 array of SSSS keys which are vertically associated.

Example:


    P = Participant (single wallet)
    O = Organization (online clearing house server)
    B = Backup (cold storage, third party)

         :HI        :LO
    FXC1_random_FXC_random_1 - P     } cookie

    FXC1_random_FXC_random_2 - P     } participant's backup
    FXC1_random_FXC_random_3 - P     } participant's backup

    FXC1_random_FXC_random_4 - O & P } participant's backup

    FXC1_random_FXC_random_5 - O
    FXC1_random_FXC_random_6 - O

    FXC1_random_FXC_random_7 - O & B } backup organization

    FXC1_random_FXC_random_8 - B     } backup organization
    FXC1_random_FXC_random_9 - B     } backup organization


The random is a long integer encoded using hashid using an alphabet that is fine tuned to not include any ambiguous character and eventually be communicated between humans without the use of computers. The salt used to hashid encode is semi-secret and is communicated to all parties.

## Tokens

Every authenticated session should contain this information:

Session:

    {
        uuid:  string
        slice: string
    }

These values are either found as private cookies or generated by a signup process. The implementation is in secretshare.clj and auth.clj (WIP)

## Backup

The backup of authentication can be made saving users email and/or providing a QR code for print and cold storage

The QR will contain:

    {
        uuid: string
        slice-1: string
        slice-2: string
        slice-3: string
    }

## Authentication flow

P = participant
O = Organization

`O <- P :` if valid cookie *login* else propose *signin* or *recover*

Signin:

`O -> P :` create NXT pass, slice it in a tuple, save in db, give new cookie, redirect to new wallet

Login:

`O -> P :` get the slice from cookie, rebuild NXT pass, access wallet

Recover:

`O <- P :` ask email

`O      :` create unique secret url ready to set cookie

`O -> P :` send secret url

`O <- P :` access url, get cookie, redirect to wallet access




# More TODO

Progress using http://swagger.io

Ring-swagger https://github.com/metosin/ring-swagger

Defining schema using Prismatic https://github.com/Prismatic/schema
