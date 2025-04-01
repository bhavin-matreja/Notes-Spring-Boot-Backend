package com.example.spring_boot_notes.controllers

import com.example.spring_boot_notes.database.model.Note
import com.example.spring_boot_notes.database.repository.NoteRepository
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/notes")
class NoteController(
    private val respository: NoteRepository
) {

    data class NoteRequest(
        val id: String?,
        val title: String,
        val content: String,
        val color: Long
    )

    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant
    )

    // POST http://localhost:8085/notes
    @PostMapping()
    fun save(
       @RequestBody body: NoteRequest
    ): NoteResponse {
        val note = respository.save(
            Note(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                content =  body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId()
            )
        )
        return note.toResponse()
    }

    // GET http://localhost:8085/notes?ownerId=123
    @GetMapping
    fun findByOwnerId(
        @RequestParam(required = true) ownerId: String
    ): List<NoteResponse> {
        return respository.findByOwnerId(ObjectId(ownerId)).map {
            it.toResponse()
        }
    }

    // DELETE http://localhost:8080/notes/123
    @DeleteMapping(
        path = ["/{id}"]
    )
    fun deleteById(@PathVariable id:String) {
        respository.deleteById(ObjectId(id))
    }

    private fun Note.toResponse(): NoteResponse {
        return NoteResponse(
            id = id.toHexString(),
            title = title,
            content = content,
            color = color,
            createdAt = createdAt
        )
    }
}