
# Https Certificate Installation

The 'certbot' role will:

* run lets-encrypt and create a publicly signed SSL cert.
* sets up a weekly renewal cronjob that will renew the SSL cert if it
    is within 30 days of expiry

For each domain running SSH, a CAA DNS record needs to be created (one time):

![Screenshot from 2019-11-19 13-06-13](https://user-images.githubusercontent.com/12397753/69196411-48980e00-0ae3-11ea-9130-61e1fd5368b3.png)
