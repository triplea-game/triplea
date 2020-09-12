
Every release contains checksum files that can be used to verify the integrity of the binary files in the release.  Currently, MD5, SHA-1, and SHA-256 checksums are provided in the files _md5sum.txt_, _sha1sum.txt_, and _sha256sum.txt_, respectively.

To verify that a downloaded binary file matches the checksum, run the appropriate checksum file through the corresponding checksum tool.  For example, to verify the SHA-256 checksum of the Unix installer for version 1.9.0.7029:

<pre>
$ <b>sha256sum -c --ignore-missing sha256sum.txt</b>
TripleA_1.9.0.7029_unix.sh: OK
</pre>

You must ensure "OK" is printed, otherwise the binary was corrupted or modified by a malicious third-party.

The `--ignore-missing` switch is recommended because it is likely you will download only a single binary file from the release (e.g. the installer for your platform).  The checksum file contains an entry for _every_ binary file in the release, and, without this switch, the checksum tool will report a warning for each file not present, which can obfuscate the output.

Additionally, every release contains the GPGv2 detached signature for each checksum file in an associated file named _\<checksum_file_name\>.asc_.  The `gpg2` tool can be used to verify that the checksum file itself has not been tampered with.

To verify that a checksum file has not been altered, you will first need to import the GPG public key of the `tripleabuilderbot` account used to create releases (you only need to do this once).  Copy the key below to a new file on your system (you must be viewing this page via HTTPS to ensure the integrity of the displayed public key):

```
-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: GnuPG v2

mQENBFnRqnIBCADT8WqVYrwHTT3aHiN2TXLq19klrI6EgkeLsvOvjFuwv0DA9VNd
OPIb/uA6lqSE4ZNkVZyQK/f7RGvDTzT2uf3L9oEZMP8PSdhEw8StYVetC8UYORA4
ehto4c+CC4NeS/H7ju9RXME3MbhFNMdyWPine9vHHrJLH4TO23DrKBLTxHSBFlca
Zmgxqeelgz+4oBoW0XSU9qy5PDmmQNpNKeeMd4/cZIYMUCO9BwWPz6bDvmSFflcA
f5Valf7Xav2mi7uMcL+grTMEXxx+OAKbH9T90TIJGWDaUrrfyVySeAYDvhF6PpNU
pVonOeU1XF8Pa2f/5ludmOfKrbgLD2IQopWfABEBAAG0L3RyaXBsZWFidWlsZGVy
Ym90IDx0cmlwbGVhYnVpbGRlcmJvdEBnbWFpbC5jb20+iQE3BBMBCAAhBQJZ0apy
AhsDBQsJCAcCBhUICQoLAgQWAgMBAh4BAheAAAoJELx6ATZbR05qSEkIAKkgqIfd
FgCewOX1By9jFkppE/XErfVGqBIxQSniKOKsM3H8VbO3tb3Kyoz7Q9J5iJWMhB5l
XALEakNBYV9tOhsVMF7Gvp8XRTFu29pfYmpNjME6nNiPzWB/WGU2fWVmijqcaAbl
bgwcPa+9rp5vadSET8hbpTs+F4gt7Pa9fGPD2cLWqlL7AQOAjSzavAer8r0/bdVg
U0bjC8f+blKnb05Fmu63dj+AuLrjVK32QbpQBr0Uzw8Ehb5LPE1vFqYh+pZzqENx
W3PcGQNupa0CrAszXKlj9YtZhl6AT47nSAZxqVnGs3rbankJIYBU44TfxGhz+YUv
mViWVYYhKOVaXnG5AQ0EWdGqcgEIAKtyS80N4qRuKktZ4Kdki0KzxyN7o8HIlVMs
7b1fEqrQ8j8LCurDfRAH+azohUScECZYJjjTIMIESRJEXcX+RwkjxGZTa49M36G6
Bm9KLGSyfO3yqzL5fw4jA5JQCLmNHXr+F7o9/D5TciZ5c+tobtduu+IcgCa2STb9
FC21CUsArHVHGrvighUJLxfQA5VpOoXBMd/WyhDmNV46z+3QHf5O/a5iT1P8ZOoh
EPFwVlQwvajLpsq8fC9hvPHq6Vk6IeCNoETzNPdfh4ZghlN4mpAaLmG1MpuDU2PY
UGu8Lqx74tpMByrUxC/18XKCoANYUbMrzQO2BlXRVRsAbprOXSMAEQEAAYkBHwQY
AQgACQUCWdGqcgIbDAAKCRC8egE2W0dOaujDB/sFCYaG9ymxUDlqow+ZTDPUHsny
O/xhdm9zp7faE0V1BazGdY65dgkj+GFzCYr5zAecz5Life8D2MFf1X00Y0hnz2Xt
zXdzCL8EG8oYHdohwXKXndsNLctkAGBLJn/5rVMLmnT/lqZ+dEg+u5j09lJFSaeI
lw/V51udVkX6gqWp46gE+kPvqU71PoUZTVpoC3+YDMNNPGMax8+r5lBpc6YjM8Cp
73E7dXS97e+6MdATh9VFJBQ78735UyOVc3s3cpKAlzKBMopBY5PpaIL2VkXX1Pf1
mJSJ8DZktFYCljOcSzXyHpg4t/V5BaJ2XZ2MRaCD7unuJyRrNznjZ8+dzFZ/
=ApjL
-----END PGP PUBLIC KEY BLOCK-----
```

Next import the key into your GPG keyring (this command assumes you saved the above public key to a file named _tripleabuilderbot-public.asc_):

<pre>
$ <b>gpg2 --import tripleabuilderbot-public.asc</b>
gpg: key BC7A01365B474E6A: public key "tripleabuilderbot &lt;tripleabuilderbot@gmail.com&gt;" imported
gpg: Total number processed: 1
gpg:               imported: 1
</pre>

Once the key is imported, you need to verify its fingerprint:

<pre>
$ <b>gpg2 --edit-key tripleabuilderbot</b>
pub  rsa2048/BC7A01365B474E6A
     created: 2017-10-02  expires: never       usage: SC
     trust: unknown       validity: unknown
sub  rsa2048/F86D2732621F466B
     created: 2017-10-02  expires: never       usage: E
[ unknown] (1). tripleabuilderbot &lt;tripleabuilderbot@gmail.com&gt;

gpg&gt; <b>fpr</b>
pub   rsa2048/BC7A01365B474E6A 2017-10-02 tripleabuilderbot &lt;tripleabuilderbot@gmail.com&gt;
 <b>Primary key fingerprint: 475F ABA4 0A16 B41B 27FB  A643 BC7A 0136 5B47 4E6A</b>
</pre>

The important line is in bold.  The fingerprint should match:

```
475F ABA4 0A16 B41B 27FB  A643 BC7A 0136 5B47 4E6A
```

If the fingerprint matches, you may optionally sign the key to validate it:

<pre>
gpg&gt; <b>sign</b>

pub  rsa2048/BC7A01365B474E6A
     created: 2017-10-02  expires: never       usage: SC
     trust: unknown       validity: unknown
 Primary key fingerprint: 475F ABA4 0A16 B41B 27FB  A643 BC7A 0136 5B47 4E6A

     tripleabuilderbot &lt;tripleabuilderbot@gmail.com&gt;

Are you sure that you want to sign this key with your
key "John Doe &lt;john.doe@example.com&gt;" (6D9A732692B2F623)

Really sign? (y/N) <b>y</b>
</pre>

Finally, exit the GPG key editor:

<pre>
gpg&gt; <b>quit</b>
Save changes? (y/N) <b>y</b>
</pre>

After importing the public key, you may now verify any of the checksum files.  For example, to verify the SHA-256 checksum file:

<pre>
$ <b>gpg2 --verify sha256sum.txt.asc</b>
gpg: assuming signed data in 'sha256sum.txt'
gpg: Signature made Thu 05 Oct 2017 06:58:39 AM EDT using RSA key ID BC7A01365B474E6A
gpg: <b>Good signature from "tripleabuilderbot &lt;tripleabuilderbot@gmail.com&gt;" [full]</b>
</pre>

The important line is in bold.  You must ensure "Good signature" from "tripleabuilderbot" is printed, otherwise the checksum file was corrupted, modified by a malicious third-party, or not signed by the TripleA development team.

Note that if you chose to not sign the `tripleabuilderbot` key when you imported it, a warning will be displayed during verification noting that the key used to sign the checksum file has not been certified along with the key fingerprint:

```
gpg: WARNING: This key is not certified with a trusted signature!
gpg:          There is no indication that the signature belongs to the owner.
Primary key fingerprint: 475F ABA4 0A16 B41B 27FB  A643 BC7A 0136 5B47 4E6A
```

This warning can be safely ignored as long as the key fingerprint matches the published value above.

