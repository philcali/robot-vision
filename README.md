# Robot Vision Control

Robot Vision Control (RVC) allows Chrome browsers to control a host machine the
same way one would through applications like RDP or VNC.

## Requirements

- [java][java] >= 1.6
- n8han's [cs][cs] (follow instructions on README)

[java]: http://java.com/en/download/index.jsp
[cs]: https://github.com/n8han/conscript#readme

## Installation

```
> cs philcali/robot-vision
```

Test it out with:

```
> rvc -j -f 100
```

Direct your browser to [http://localhost:8080/robot-vision.html][locally]

[locally]: http://localhost:8080/robot-vision.html

## Streaming vs Reloading

RVC allows for two specific forms of _reloading_ the desktop image. The first
way is a client side loop reloading the image. The second is the _jpeg_ camera
type streaming to the browser, where the server pushes images to clients.

The default behavior is to have javascript reload the scene, but testing showed
that jpeg camera streams proved to be more fluid. Don't use this method if
you know clients browsers don't support it.

## Using a Chrome Extension

Control communication must be allowed via passing a _key_ to an open web socket
connection. This means it's possible to __not__ serve the control code (with `-n`),
and inject the needed code using a Chrome Extension.

The extension is available at [robot-chrome][vision-ext].

[vision-ext]: https://github.com/philcali/robot-chrome

## Options

```
Usage: rvc [OPTIONS]

OPTIONS

-b (0.0.0.0)
--bind-address (0.0.0.0)             bind address

-c
--clear-keys                         Clears stuck keyboard inputs.

-f framerate 30
--framerate framerate 30             If in jpeg camera mode, push image data at
                                     specified framerate

-g
--gen-secret                         generate a secret key to be passed to
                                     socket program

-i 8080
--inet-port 8080                     internet port

-j
--jpeg-camera                        Serves image data via jpeg camera
                                     transport

-n
--no-connect                         don't serve up connection js (ideal if
                                     using Chrome extension to connect)

-p <none>
--password <none>                    password to auth

-s
--secured                            https server (http)

-u <none>
--user <none>                        user to auth

-v <viewer password>
--viewer-password <viewer password>  separate password for the 'viewer' user
                                     (leave blank for open)
```

## Known Issues

- Very rarely in testing, I found that keyboard inputs would _stick_. The only
way to fix this at the moment, is to re-run `rvc` with the `-c` or `--clear-keys` flag.
- Dual monitor support does not currently work, but should be easy enough to add. 

## TODO's

- Popout interface for dynamic controls (mainly controlling scales and quality)
- interface for uploading and downloading files

## License

The MIT.
