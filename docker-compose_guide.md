# Docker Compose Guide

## Environment
Copy the .env.example file and name it .env:
```bash
cp .env.example .env
```
DO NOT COMMIT .env.

Fill in the brackets in .env with your own contents.

To load your .env file as environment variables in the VM, run:
```bash
export $(grep -v '^#' .env | xargs)
```


## Starting the containers
Run:
```bash
docker compose up --build -d
```
To check status, run:
```bash
docker compose ps
```
You should see all three containers: frontend, backend, and database up and running.

## Initializing the database schema
The database container needs the schema loaded.

This needs to be done once per developer on setup, and then subsequently rerun when the database is dropped or the schema is updated.
```bash
docker compose exec -T database   mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < ./database/schema.sql
```

For existing databases, if signup fails because `wallet_private_key` is missing, run:
```bash
docker compose exec -T database mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "ALTER TABLE UserWallets ADD COLUMN wallet_private_key VARCHAR(255) NOT NULL DEFAULT '' AFTER wallet_address;"
```

To open an interactive MySQL shell, run:
```bash
docker compose exec database mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
```

Once in the MySQL shell, run SQL commands such as:
```sql
SHOW TABLES:
```
or
```sql
SELECT * FROM Users;
```

To exit the shell, simply type:
```sql
exit;
```

## SSH Tunnels
SSH tunnels allow you to redirect ports through ssh to ports on other machines.

This allows you to, for instance, see the frontend in a browser on your local machine.

These commands should be run on your local machine, NOT the VM. Either exit from the VM or open a new terminal.

Additionally, the docker containers need to be up and running for the below to work.

### Frontend
Run:
```bash
ssh -L 3000:localhost:3000 {cslogin}@cs506x32.cs.wisc.edu
```
Where:
- {cslogin} is your normal cslogin

To access locally, you can visit 'http://localhost:3000' in your browser.

### Backend
Run:
```bash
ssh -L 3001:localhost:3001 {cslogin}@cs506x32.cs.wisc.edu
```
Where:
- {cslogin} is your normal cslogin

To access locally, visit 'http://localhost:3001' in your browser, appending any API endpoints to the URL to view them.

### Database
Run:
```bash
ssh -L 3002:localhost:3002 {cslogin}@cs506x32.cs.wisc.edu
```
Where:
- {cslogin} is your normal cslogin

To access the MySQL engine, run:
```bash
mysql -h 127.0.0.1 -P 3002 -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
```

## Working Across Containers
The frontend can connect to the backend at "backend:8080"

The backend can connect to the database at "database:3306"

## Stopping the environment
To stop all containers, run:
```bash
docker compose down
```
or kill them manually with docker kill.

This will not remove your local database, to remove your local database, run:
```bash
docker compose down -v
```
When you run docker compose up again, docker will rebuild the database based on database/schema.sql.
