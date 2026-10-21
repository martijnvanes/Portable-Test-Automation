# Updating the TAS on the Synology

Merging to `master` builds and publishes a new image, but the Synology does not pick it up by
itself, and Container Manager's Build action does not pull a newer image. Two ways to update:

## Task Scheduler (no SSH)

One-time setup in the DSM web interface:

1. Control Panel > Task Scheduler > Create > Triggered Task > User-defined script.
2. General: give it a name such as `Update TAS`, user `root`, event `Boot-up`. Untick **Enabled** so
   it never runs on its own.
3. Task Settings > Run command:

   ```sh
   cd /volume1/docker/tas && docker compose pull && docker compose up -d
   ```

   Replace `/volume1/docker/tas` with the project folder that Container Manager uses for project
   `tas`. It is the folder that holds `docker-compose.yml`. There is no `sudo`, the task runs as root.
4. Save.

To update: wait until the "Build and publish image" workflow on GitHub is green, select the task
in Task Scheduler and click **Run**. Task Scheduler > Action > View Result shows the output.

To update on a schedule instead, create a Scheduled Task with the same command, for example nightly.

## SSH

In the project folder:

```sh
sudo docker compose pull && sudo docker compose up -d
```

The pull needs the Synology to be logged in to GHCR (the image is private). If the task fails with
an authentication error, log in as root over SSH once (`sudo docker login ghcr.io`, with a token
that can read packages); the task runs as root and uses that login.
