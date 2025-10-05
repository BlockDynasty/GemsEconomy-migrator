From GemsEconomy 4.9.2
## Only for MySQL to MySQL

#### Requisites:
- As a requirement, no players should be connected or interacting with the plugins.

#### Process:
1. First, make sure you have the main plugin **BlockDynasty-Economy** installed and its database properly initialized.  
2. Install the migration plugin in your plugins folder: `GemsEconomy-migrator.jar`.  
3. Start the server.  
4. Once the `GemsEconomy-migrator` folder has been generated, stop the server.  
5. Open the `config.yaml` file and edit the necessary information for both databases: **GemsEconomy** and **BlockDynasty-Economy**.  
6. Start the server again.  
7. In the console, type: `/gemsMigrator start`.  
8. Once the process completes successfully, stop the server and delete both `GemsEconomy-migrator.jar` and the `GemsEconomy-migrator` folder.






