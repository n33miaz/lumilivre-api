package br.com.lumilivre.api.dto.book;

public interface BookListItemProjection {

    String getStatus();
    String getCopyCode();
    String getIsbn();
    String getDeweyCode();
    String getTitle();
    String getGenre();
    String getAuthor();
    String getPublisher();
    String getPhysicalLocation();
}
