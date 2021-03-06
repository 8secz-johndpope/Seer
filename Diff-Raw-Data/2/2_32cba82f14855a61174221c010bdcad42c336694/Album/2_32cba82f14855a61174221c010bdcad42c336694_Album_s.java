 /*
  * Sone - Album.java - Copyright © 2011 David Roden
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.pterodactylus.sone.data;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.UUID;
 
 import net.pterodactylus.util.validation.Validation;
 
 /**
  * Container for images that can also contain nested {@link Album}s.
  *
  * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
  */
 public class Album implements Fingerprintable {
 
 	/** The ID of this album. */
 	private final String id;
 
 	/** The Sone this album belongs to. */
 	private Sone sone;
 
 	/** Nested albums. */
 	private final List<Album> albums = new ArrayList<Album>();
 
 	/** The images in this album. */
 	private final List<Image> images = new ArrayList<Image>();
 
 	/** The parent album. */
 	private Album parent;
 
 	/** The name of this album. */
 	private String name;
 
 	/** The description of this album. */
 	private String description;
 
 	/**
 	 * Creates a new album with a random ID.
 	 */
 	public Album() {
 		this(UUID.randomUUID().toString());
 	}
 
 	/**
 	 * Creates a new album with the given ID.
 	 *
 	 * @param id
 	 *            The ID of the album
 	 */
 	public Album(String id) {
 		Validation.begin().isNotNull("Album ID", id).check();
 		this.id = id;
 	}
 
 	//
 	// ACCESSORS
 	//
 
 	/**
 	 * Returns the ID of this album.
 	 *
 	 * @return The ID of this album
 	 */
 	public String getId() {
 		return id;
 	}
 
 	/**
 	 * Returns the Sone this album belongs to.
 	 *
 	 * @return The Sone this album belongs to
 	 */
 	public Sone getSone() {
 		return sone;
 	}
 
 	/**
 	 * Sets the owner of the album. The owner can only be set as long as the
 	 * current owner is {@code null}.
 	 *
 	 * @param sone
 	 *            The album owner
 	 * @return This album
 	 */
 	public Album setSone(Sone sone) {
		Validation.begin().isNull("Current Album Owner", this.sone).isNotNull("New Album Owner", sone).check().isEqual("New Album Owner", sone, this.sone).check();
 		this.sone = sone;
 		return this;
 	}
 
 	/**
 	 * Returns the nested albums.
 	 *
 	 * @return The nested albums
 	 */
 	public List<Album> getAlbums() {
 		return new ArrayList<Album>(albums);
 	}
 
 	/**
 	 * Adds an album to this album.
 	 *
 	 * @param album
 	 *            The album to add
 	 */
 	public void addAlbum(Album album) {
 		Validation.begin().isNotNull("Album", album).check().isEqual("Album Owner", album.sone, sone).isNull("Album Parent", album.parent).check();
 		albums.add(album);
 		album.setParent(this);
 	}
 
 	/**
 	 * Removes an album from this album.
 	 *
 	 * @param album
 	 *            The album to remove
 	 */
 	public void removeAlbum(Album album) {
 		Validation.begin().isNotNull("Album", album).check().isEqual("Album Owner", album.sone, sone).isEqual("Album Parent", album.parent, this).check();
 		albums.remove(album);
 		album.removeParent();
 	}
 
 	/**
 	 * Returns the images in this album.
 	 *
 	 * @return The images in this album
 	 */
 	public List<Image> getImages() {
 		return new ArrayList<Image>(images);
 	}
 
 	/**
 	 * Adds the given image to this album.
 	 *
 	 * @param image
 	 *            The image to add
 	 */
 	public void addImage(Image image) {
 		Validation.begin().isNotNull("Image", image).check().isEqual("Image Owner", image.getSone(), sone).check();
 		images.add(image);
 	}
 
 	/**
 	 * Removes the given image from this album.
 	 *
 	 * @param image
 	 *            The image to remove
 	 */
 	public void removeImage(Image image) {
 		Validation.begin().isNotNull("Image", image).check().isEqual("Image Owner", image.getSone(), sone).check();
 		images.remove(image);
 	}
 
 	/**
 	 * Returns the parent album of this album.
 	 *
 	 * @return The parent album of this album, or {@code null} if this album
 	 *         does not have a parent
 	 */
 	public Album getParent() {
 		return parent;
 	}
 
 	/**
 	 * Sets the parent album of this album.
 	 *
 	 * @param parent
 	 *            The new parent album of this album
 	 * @return This album
 	 */
 	protected Album setParent(Album parent) {
 		Validation.begin().isNotNull("Album Parent", parent).check();
 		this.parent = parent;
 		return this;
 	}
 
 	/**
 	 * Removes the parent album of this album.
 	 *
 	 * @return This album
 	 */
 	protected Album removeParent() {
 		Validation.begin().isNotNull("Album Parent", parent).check();
 		this.parent = null;
 		return this;
 	}
 
 	/**
 	 * Returns the name of this album.
 	 *
 	 * @return The name of this album
 	 */
 	public String getName() {
 		return name;
 	}
 
 	/**
 	 * Sets the name of this album.
 	 *
 	 * @param name
 	 *            The name of this album
 	 * @return This album
 	 */
 	public Album setName(String name) {
 		Validation.begin().isNotNull("Album Name", name).check();
 		this.name = name;
 		return this;
 	}
 
 	/**
 	 * Returns the description of this album.
 	 *
 	 * @return The description of this album
 	 */
 	public String getDescription() {
 		return description;
 	}
 
 	/**
 	 * Sets the description of this album.
 	 *
 	 * @param description
 	 *            The description of this album
 	 * @return This album
 	 */
 	public Album setDescription(String description) {
 		Validation.begin().isNotNull("Album Description", description).check();
 		this.description = description;
 		return this;
 	}
 
 	//
 	// FINGERPRINTABLE METHODS
 	//
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public String getFingerprint() {
 		StringBuilder fingerprint = new StringBuilder();
 		fingerprint.append("Album(");
 		fingerprint.append("ID(").append(id).append(')');
 		fingerprint.append("Name(").append(name).append(')');
 		fingerprint.append("Description(").append(description).append(')');
 
 		/* add nested albums. */
 		fingerprint.append("Albums(");
 		for (Album album : albums) {
 			fingerprint.append(album.getFingerprint());
 		}
 		fingerprint.append(')');
 
 		/* add images. */
 		fingerprint.append("Images(");
 		for (Image image : images) {
 			fingerprint.append(image.getFingerprint());
 		}
 		fingerprint.append(')');
 
 		fingerprint.append(')');
 		return fingerprint.toString();
 	}
 
 	//
 	// OBJECT METHODS
 	//
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public boolean equals(Object object) {
 		if (!(object instanceof Album)) {
 			return false;
 		}
 		Album album = (Album) object;
 		return id.equals(album.id);
 	}
 
 }
