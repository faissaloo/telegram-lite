package mtproto.send;
import bouncycastle.BigInteger;

import support.ByteArrayPlus;
import support.Integer128;

import crypto.SHA1;
import crypto.AES256IGE;
import crypto.SecureRandomPlus;

import mtproto.CombinatorIds;
import mtproto.Serialize;
import mtproto.UnencryptedRequest;

public class SendSetClientDHParams {
  ByteArrayPlus message_data;

  public SendSetClientDHParams(Integer128 nonce, Integer128 server_nonce, long retry_id, int group_generator, BigInteger diffie_hellman_prime, BigInteger b, byte[] tmp_aes_key, byte[] tmp_aes_iv) {
    message_data = new ByteArrayPlus();
    message_data.append_int(CombinatorIds.set_client_DH_params);
    message_data.append_Integer128(nonce);
    message_data.append_Integer128(server_nonce);
    BigInteger group_generator_power_b = BigInteger.valueOf(group_generator).modPow(b, diffie_hellman_prime);
    
    byte[] inner_data = (new ByteArrayPlus())
      .append_int(CombinatorIds.client_DH_inner_data)
      .append_Integer128(nonce)
      .append_Integer128(server_nonce)
      .append_long(retry_id)
      .append_raw_bytes(Serialize.serialize_bytes(group_generator_power_b.magnitudeToBytes()))
      .toByteArray();

    byte[] inner_data_hash = (new SHA1()).digest(inner_data);
    
    SecureRandomPlus random_number_generator = new SecureRandomPlus();
    byte[] data_with_hash = new ByteArrayPlus()
      .append_raw_bytes(inner_data_hash)
      .append_raw_bytes(inner_data)
      .pad_to_alignment(16, random_number_generator)
      .toByteArray();
      
    byte[] encrypted_data = AES256IGE.encrypt(tmp_aes_key, tmp_aes_iv, data_with_hash);
    message_data.append_raw_bytes(Serialize.serialize_bytes(encrypted_data));
  }
  
  public void send() {
    (new UnencryptedRequest(message_data.toByteArray())).send();
  }
}