package com.schuwalow.todo.http

import com.schuwalow.todo._
import com.schuwalow.todo.repository._
import io.circe.{Decoder, Encoder}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import zio._
import zio.interop.catz._
import zio.logging.log

object TodoService {

  def routes[R <: TodoRepository]
            (rootUri: String)
            : HttpRoutes[RIO[R, *]] = {

    type TodoTask[A] = RIO[R, A]

    val dsl: Http4sDsl[TodoTask] = Http4sDsl[TodoTask]
    import dsl._

    implicit def circeJsonDecoder[A: Decoder]: EntityDecoder[TodoTask, A] = jsonOf[TodoTask, A]

    implicit def circeJsonEncoder[A: Encoder]: EntityEncoder[TodoTask, A] = jsonEncoderOf[TodoTask, A]

    HttpRoutes.of[TodoTask] {
      case GET -> Root / LongVar(id) =>
        for {
          todo     <- TodoRepository.getById(TodoId(id))
          response <- todo.fold(NotFound())(x => Ok(TodoItemWithUri(rootUri, x)))
        } yield response

      case GET -> Root =>
        log.info(s"--------------------------------------[TodoService.routes.of]-------------------------------------- Get")
        Ok(TodoRepository.getAll.map(_.map(TodoItemWithUri(rootUri, _))))

      case req @ POST -> Root =>
        log.info(s"--------------------------------------[TodoService.routes.of]-------------------------------------- Post, req: $req")
        req.decode[TodoItemPostForm] { todoItemForm =>
          TodoRepository
            .create(todoItemForm)
            .map(TodoItemWithUri(rootUri, _))
            .flatMap(Created(_))
        }

      case DELETE -> Root / LongVar(id) =>
        log.info(s"--------------------------------------[TodoService.routes.of]-------------------------------------- Delete, id=$id")
        for {
          item   <- TodoRepository.getById(TodoId(id))
          result <- item
                      .map(x => TodoRepository.delete(x.id))
                      .fold(NotFound())(_.flatMap(Ok(_)))
        } yield result

      case DELETE -> Root =>
        log.info(s"--------------------------------------[TodoService.routes.of]-------------------------------------- Delete all")
        TodoRepository.deleteAll *> Ok()

      case req @ PATCH -> Root / LongVar(id) =>
        log.info(s"--------------------------------------[TodoService.routes.of]-------------------------------------- Patch, id=$id")
        req.decode[TodoItemPatchForm] { updateForm =>
          for {
            update   <- TodoRepository.update(TodoId(id), updateForm)
            response <- update.fold(NotFound())(x => Ok(TodoItemWithUri(rootUri, x)))
          } yield response
        }
    }
  }
}
