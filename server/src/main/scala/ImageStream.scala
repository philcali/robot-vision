package capture
package server

import unfiltered.request._
import unfiltered.response._
import unfiltered.netty._

import org.jboss.netty.handler.codec.http.{
  HttpHeaders,
  DefaultHttpChunk,
  DefaultHttpChunkTrailer
}
import org.jboss.netty.channel.group.{
  DefaultChannelGroup,
  ChannelGroupFuture
}
import org.jboss.netty.channel.{
  Channel,
  ChannelFuture,
  ChannelFutureListener
}
import org.jboss.netty.buffer.ChannelBuffers

// Code greatly inspired by n8han/shouty
// https://github.com/n8han/shouty/blob/master/src/main/scala/stream.scala
object ImageStream extends Interface {
  val boundary = "desktrotopio"

  val MixedReplace =
    Ok ~>
    unfiltered.response.Connection(HttpHeaders.Values.CLOSE) ~>
    ContentType("multipart/x-mixed-replace; boundary=--%s".format(boundary))

  val interface =
    Ok ~>
    ContentType("text/javascript") ~>
    ResponseString("")

  val listeners = new DefaultChannelGroup

  def preload = {
    // Imagine this file will actually be different
    // For now we'll just blank it out
    case req @ Path("/interface.js") => req.respond(interface)
    case req @ Path(Seg("image" :: DesktopImage(x, y, q, p) :: Nil)) =>
      // TODO: What about the controller changing service settings?
      val initial = req.underlying.defaultResponse(MixedReplace)
      val ch = req.underlying.event.getChannel
      ch.write(initial).addListener { () =>
        listeners.add(ch)
        ImageService ! Write
      }
  }

  // Write complete boundary jpeg data
  def write(data: Array[Byte]) {
    val border = "\n\n--%s\nContent-Type: image/jpeg\nContent-Length: %d\n\n".format(
      boundary, data.length
    ).getBytes
    val together = border ++ data
    listeners.write(new DefaultHttpChunk(
      ChannelBuffers.copiedBuffer(together, 0, together.length)
    ))
  }

  implicit def block2listener[T](block: () => T): ChannelFutureListener = {
    new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) { block() }
    }
  }
}
