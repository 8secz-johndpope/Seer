 /*
  * Copyright 2013 Yubico AB
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.yubico.bitcoin.api;
 
 import java.io.IOException;
 
 /**
  * Interface to the ykneo-bitcoin applet running on a YubiKey NEO.
  * <p/>
  * Keys used are in the <a href="https://en.bitcoin.it/wiki/BIP_0032">BIP 32</a> format.
  * A single extended key pair is stored on the YubiKey NEO, which is used to derive sub keys used for signing.
  * <p/>
 * Though BIP 32 supports a tree hierarchy of keys, ykneo-bitcoin only supports a flat hierarchy (though the stored
 * extended key pair does not need to be a root node). The getPublicKey and sign methods work on the sub keys m/i, where
 * m is the stored extended key pair and i is the index used in the methods. It is thus possible to get public keys and
 * sign data using private keys from the sub keys known as m/0...n where n is a 32 bit integer. Private key derivation
 * is supported, by setting the first (sign-) bit of index, as per the BIP 32 specification.
  * <p/>
  * Example:
  * YkneoBitcoin neo = ...
  * byte[] extendedPrivateMasterKey m = ...
 * neo.importExtendedKeyPair(m, false).get(); //neo now holds the master key pair m.
  * <p/>
 * neo.getPublicKey(0); //This returns the uncompressed public key from sub key m/0
 * int index = 4711;
 * index |= 0x80000000
 * neo.sign(index, hash); //This returns the signature of hash signed by m/4711'
  */
 public interface YkneoBitcoin {
     /**
      * Gets the version of the ykneo-bitcoin applet that is loaded on the YubiKey NEO.
      * The format is "major.minor.micro".
      *
      * @return The ykneo-bitcoin applet version.
      */
     String getAppletVersion();
 
     /**
      * Checks if an extended private key has been loaded onto the device or not.
      * @return
      */
     boolean isKeyLoaded();
 
     /**
      * Unlocks user mode of operation. If the incorrect PIN is given too many times, the mode will be locked.
      *
      * @param pin The PIN code to unlock user mode.
      * @throws IncorrectPINException
      * @throws IOException
      */
     void unlockUser(String pin) throws IncorrectPINException, IOException;
 
     /**
      * Unlocks admin mode of operation. If the incorrect PIN is given too many times, the mode will be locked.
      *
      * @param pin The PIN code to unlock admin mode.
      * @throws IncorrectPINException
      * @throws IOException
      */
     void unlockAdmin(String pin) throws IncorrectPINException, IOException;
 
     /**
      * Check to see if user mode is unlocked.
      *
      * @return True if user mode is unlocked, false if not.
      */
     boolean isUserUnlocked();
 
     /**
      * Check to see if admin mode is unlocked.
      *
      * @return True if admin mode is unlocked, false if not.
      */
     boolean isAdminUnlocked();
 
     /**
      * Changes the user PIN. Does not require user mode to be unlocked.
      * After successfully setting the PIN, the mode will be locked.
      *
      * @param oldPin The current user PIN.
      * @param newPin The new user PIN to set.
      * @throws IncorrectPINException
      * @throws IOException
      */
     void setUserPin(String oldPin, String newPin) throws IncorrectPINException, IOException;
 
     /**
      * Changes the admin PIN. Does not require admin mode to be unlocked.
      * After successfully setting the PIN, the mode will be locked.
      *
      * @param oldPin The current admin PIN,
      * @param newPin The new admin PIN to set.
      * @throws IncorrectPINException
      * @throws IOException
      */
     void setAdminPin(String oldPin, String newPin) throws IncorrectPINException, IOException;
 
     /**
      * Re-sets and unblocks the user PIN. Can be used if the user PIN is lost.
      * Requires admin mode to be unlocked.
      *
      * @param newPin The new user PIN to set.
      * @throws PinModeLockedException
      * @throws IOException
      */
     void resetUserPin(String newPin) throws PinModeLockedException, IOException;
 
     /**
      * Sets the maximum number of PIN entry attempts before locking the user PIN.
      * Requires admin mode to be unlocked.
      *
      * NOTE: This requires admin mode, even though it sets the retry counter for the user PIN!
      *
      * @param attempts the number of failed attempts before lock, must be 1-15.
      * @throws PinModeLockedException
      * @throws IOException
      * @since ykneo-bitcoin 0.1.0
      */
     void setUserRetryCount(int attempts) throws PinModeLockedException, IOException;
 
     /**
      * Sets the maximum number of PIN entry attempts before locking the admin PIN.
      * Requires admin mode to be unlocked.
      *
      * @param attempts the number of failed attempts before lock, must be 1-15.
      * @throws PinModeLockedException
      * @throws IOException
      * @since ykneo-bitcoin 0.1.0
      */
     void setAdminRetryCount(int attempts) throws PinModeLockedException, IOException;
 
     /**
      * Gets the 13 byte BIP 32 key header for the stored extended private key.
      *
      * @return version(4) | depth(4) | parents fingerprint(4) | child number(4)
      * @throws PinModeLockedException
      * @throws IOException
      * @throws NoKeyLoadedException
      */
     byte[] getHeader() throws PinModeLockedException, IOException, NoKeyLoadedException;
 
     /**
      * Gets the public key obtained by deriving a sub key from the master key pair using the given index.
      * Requires user mode to be unlocked.
      *
      * @param compress True to return a compressed public key, false to return the uncompressed public key.
      * @param index    The index of the derived sub key to get.
      * @return A 65 (uncompressed) or 33 (compressed) byte public key.
      * @throws PinModeLockedException
      * @throws UnusableIndexException
      * @throws IOException
      * @throws NoKeyLoadedException
      */
     byte[] getPublicKey(boolean compress, int... index) throws PinModeLockedException, UnusableIndexException, IOException, NoKeyLoadedException;
 
     /**
      * Signs the given hash using the private key obtained by deriving a sub key from the master key pair using the given index.
      * Requires user mode to be unlocked.
      *
      * @param hash  The 32 byte hash to sign.
      * @param index The index of the derived sub key to sign with.
      * @return A digital signature.
      * @throws PinModeLockedException
      * @throws UnusableIndexException
      * @throws IOException
      * @throws NoKeyLoadedException
      */
     byte[] sign(byte[] hash, int... index) throws PinModeLockedException, UnusableIndexException, IOException, NoKeyLoadedException;
 
     /**
      * Generates a new master key pair randomly, overwriting any existing key pair stored on the device.
      * The allowExport flag determines if the extended public key can later be exported or not.
      * The returnPrivateKey flag determines if the generated key should be returned from the device (for backup purposes) or not.
      * Requires admin mode to be unlocked.
      *
      * @param allowExport      Sets the allowExport flag permitting the extended public key to be exported.
      * @param returnPrivateKey When true, the generated extended private key is returned, when false, an empty byte[] is returned.
      * @param testnetKey       When true, the generated key will be for testnet use.
      * @return A BIP 32 formatted extended private key, if returnPrivateKey is set.
      * @throws PinModeLockedException
      * @throws IOException
      */
     byte[] generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey, boolean testnetKey) throws PinModeLockedException, IOException;
 
     /**
      * Imports a new extended key pair, overwriting any existing key pair stored on the device.
      * The allowExport flag determines if the extended public key can later be exported or not.
      * Requires admin mode to be unlocked.
      *
      * @param extendedPrivateKey A BIP 32 formatted extended private key to be imported.
      * @param allowExport        Sets the allowExport flag permitting the extended public key to be exported.
      * @throws PinModeLockedException
      * @throws IOException
      */
     void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, IOException;
 
     /**
      * Exports the stored extended public key which can be used for the creation of read-only wallets.
      * Unless the allowExport flag was set when the key was generated or imported, this method will fail.
      * Requires admin mode to be unlocked.
      *
      * @return A BIP 32 formatted extended public key.
      * @throws PinModeLockedException
      * @throws IOException
      * @throws OperationNotPermittedException
      * @throws NoKeyLoadedException
      */
     byte[] exportExtendedPublicKey() throws PinModeLockedException, IOException, OperationNotPermittedException, NoKeyLoadedException;
 }
