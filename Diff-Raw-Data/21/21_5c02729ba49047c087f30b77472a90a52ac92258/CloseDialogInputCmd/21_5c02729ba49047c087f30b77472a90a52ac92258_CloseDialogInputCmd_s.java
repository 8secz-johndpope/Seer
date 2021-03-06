 /*
  * This file is part of the Illarion Client.
  *
  * Copyright © 2012 - Illarion e.V.
  *
  * The Illarion Client is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * The Illarion Client is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with the Illarion Client.  If not, see <http://www.gnu.org/licenses/>.
  */
 package illarion.client.net.client;
 
 import illarion.client.net.CommandList;
 import illarion.client.net.NetCommWriter;
 
 /**
  * Client Command: Send a text that was requested by the server and typed in by the player ( {@link
  * illarion.client.net.CommandList#CMD_CLOSE_DIALOG_INPUT}).
  *
  * @author Martin Karing &lt;nitram@illarion.org&gt;
  */
 public final class CloseDialogInputCmd
         extends AbstractCommand {
     /**
      * The ID that was send by the server to initiate text input.
      */
    private long dialogID;
 
     /**
      * The text that is send to the server.
      */
     private String text;
 
     /**
      * Default constructor for the text response command.
      */
     public CloseDialogInputCmd() {
         super(CommandList.CMD_CLOSE_DIALOG_INPUT);
     }
 
     /**
      * Create a duplicate of this text response command.
      *
      * @return new instance of this command
      */
     @Override
     public CloseDialogInputCmd clone() {
         return new CloseDialogInputCmd();
     }
 
     /**
      * Encode the data of this text response command and put the values into the buffer.
      *
      * @param writer the interface that allows writing data to the network communication system
      */
     @Override
     public void encode(final NetCommWriter writer) {
        writer.writeUInt(dialogID);
         writer.writeUByte((byte) 0xFF);
         writer.writeString(text);
     }
 
     /**
      * Clean up the command before put it back into the recycler for later reuse.
      */
     @Override
     public void reset() {
         text = null;
     }
 
     /**
      * Set the ID of the dialog that supplied the text.
      *
      * @param id the id of the dialog
      */
    public void setDialogId(final long id) {
         dialogID = id;
     }
 
     /**
      * Set the text that shall be send to the server.
      *
      * @param sayText the text that is send
      */
     public void setText(final String sayText) {
         text = sayText;
     }
 
     /**
      * Get the data of this text response command as string.
      *
      * @return the data of this command as string
      */
     @SuppressWarnings("nls")
     @Override
     public String toString() {
         return toString("Text Response: " + text);
     }
 }
