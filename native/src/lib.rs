use base64::{encode, decode, DecodeError};
use jni::{objects::{JClass, JString}, sys::jstring, JNIEnv};
use serde::{Deserialize, Serialize};
use serde_json::Error as SerdeError;

#[no_mangle]
pub extern "system" fn Java_xyz_holocons_mc_headtexturefixer_Native_normalizeTexture(
    env: JNIEnv,
    _class: JClass,
    string: JString,
) -> jstring {
    let input: String = env.get_string(string).expect("Couldn't get java string!").into();
    let normalized = normalize(&input).unwrap_or(input);
    let output = env.new_string(normalized).expect("Couldn't create java string!");

    output.into_inner()
}

fn normalize(base64_in: &str) -> Result<String, NormalizeError> {
    match strip(&base64_in) {
        Err(NormalizeError::Base64(DecodeError::InvalidLength)) => strip(&base64_in[..base64_in.len() - 1]),
        result => result,
    }
}

fn strip(base64_in: &str) -> Result<String, NormalizeError> {
    let decoded = decode(&base64_in).map_err(|e| NormalizeError::Base64(e))?;
    let json_in: Base64Texture = serde_json::from_slice(&decoded).map_err(|e| NormalizeError::Json(e))?;
    let json_out = serde_json::to_string(&json_in).map_err(|e| NormalizeError::Json(e))?;
    let encoded = encode(&json_out);

    Ok(encoded)
}

#[derive(Debug)]
enum NormalizeError {
    Base64(DecodeError),
    Json(SerdeError),
}

#[derive(Debug, Deserialize, Serialize)]
struct Base64Texture {
    #[serde(rename = "timestamp", skip)]
    _timestamp: u64,
    #[serde(rename = "profileId", skip)]
    _profile_id: String,
    #[serde(rename = "profileName", skip)]
    _profile_name: String,
    #[serde(rename = "signatureRequired", skip)]
    _signature_required: bool,
    textures: Textures,
}

#[derive(Debug, Deserialize, Serialize)]
struct Textures {
    #[serde(rename = "SKIN")]
    skin: Skin,
}

#[derive(Debug, Deserialize, Serialize)]
struct Skin {
    url: String,
    #[serde(rename = "metadata", skip)]
    _metadata: Metadata,
}

#[derive(Debug, Default, Deserialize, Serialize)]
struct Metadata {
    model: String,
}
