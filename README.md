# Bastet

A basic CRUD rest api wrapper for managing configuration and secrets.

## CRUD API: V1

route: /api/v1

### API: V1: admin

All the data is stored with zero knowledge encryption.
You can provide/change the encryption key with the unlock endpoint.

route: /admin

- unlock -> PUT /unlock -> returns 201 status code

  body:

    ```json
    {
        "key": "key"
    }
    ```

- read user -> GET /user/{username} -> returns 200 status code

- write user -> PUT /user/{username} -> returns 201 status code

  body:

    ```json
    {
        "email": null,
        "password": "test123",
        "roles": [
            {
                "name": "ADMIN",
                "privileges": [
                    {
                        "name": "SUDO"
                    }
                ]
            }
        ],
        "lastLogin": 0,
        "locked": false,
        "enabled": true
    }
    ```

- list keys -> GET /list/keys/{key} -> returns 200 status code with a list of keys

### API: V1: secrets

route: /secrets

It performs only 3 operations: `read`, `write`, and `delete`.

- read -> GET /{key} -> returns the secret value with 200 status code

- write -> POST /{key} -> returns 201 status code

  body:

    ```json
    {
        "secret1": "value1"
    }
    ```

- delete -> DELETE /{key} -> returns 204 status code

## Configuration

env vars:

- BASTET_CONFIG: Configuration in json format or Path to the configuration file, default is bastet.json.

```json
{
    "server": {},
    "physical": {}
}
```

### Server

```json
{
    "port": "Port",
    "root-password": "Root password",
    "auth-header-key": "Auth header key",
    "ssl-key-pem": "PEM key",
    "ssl-cert-pem": "PEM cert"
}
```

### Storage

In ansible deployment, `/app/bastet/data` is mounted in docker container in same path.
So, the sqlite file should be in `/app/bastet/data`. or else I need to change the `./ansible/playbook.yml`.

```json
{
    "type": "StorageType",
    "master-key": "<Base 64 encoded 32 byte key>:$:<Base 64 encoded 16 byte IV>, this is not recommended, set this key by using the unlock endpoint. Is Set to `random` then a random key will be generated.",
    "config": {}
}
```

- `SQLITE`: Storage in a sqlite database.

    ```json
        {
            "path": "path to the sqlite file"
        }
    ```

- `LIBSQL`: Storage in a libsql database.

    ```json
        {
            "serverUrl": "Server URL is a full URL including schema and port. For example: https://test-test.turso.io",
            "token": "Token"
        }
    ```

- `postgres`: Storage in a postgres database. (Because I also want to play fancy corporate unicorn startup idea with
  horizontal scaling for my 3 users)

    ```json
        {
            "host": "host, default is 127.0.0.1",
            "port": "port, default is 5432",
            "user": "user, default is bastet",
            "password": "password, default is bastet",
            "dbname": "dbname, default is bastet"
        }
    ```

## Deployment

Add all the variables in `ansible/inventory.yml` and run the following command.
Ansible vault password is in bitwarden `ansible/vault_pass.sh`.

In `ansible/inventory.yml`, the following variables are required.

```yaml
---
all:
  vars:
    bastet_config: "Bastet configuration in yaml/json format"
```

Start the deployment with the following command.

```sh
./ansible/deploy.sh
```

## Development

### Setup

For ansible vault diff.

Add the following line in `~/.gitattributes`.

```gitignore
ansible/inventory.yml diff=ansible-vault merge=binary
```

And then run the following command.

```sh
git config diff.ansible-vault.textconv "ansible-vault view"
```

## Backup

In the roadmap, after I set up a factory for milk bottle, seat belt and helmet, I will implement a backup mechanism for
the secrets.
