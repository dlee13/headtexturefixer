use std::{
    alloc::{self as std_alloc, Layout},
    slice, str,
};

use base64::prelude::{Engine as _, BASE64_STANDARD_NO_PAD, BASE64_URL_SAFE_NO_PAD};
use serde::{Deserialize, Serialize};

#[no_mangle]
pub unsafe extern "C" fn alloc(len: usize) -> *mut u8 {
    std_alloc::alloc(Layout::array::<u8>(len).expect("Layout error"))
}

#[no_mangle]
pub unsafe extern "C" fn dealloc(ptr: *mut u8, len: usize) {
    std_alloc::dealloc(ptr, Layout::array::<u8>(len).expect("Layout error"));
}

#[no_mangle]
pub unsafe extern "C" fn normalize_texture(ptr: *mut u8, len: usize) -> usize {
    let buffer = slice::from_raw_parts_mut(ptr, len);
    let output = strip(&buffer);
    buffer[..output.len()].copy_from_slice(&output);
    output.len()
}

fn strip(input: &[u8]) -> Vec<u8> {
    let mut vec = input.to_owned();
    vec.retain(|&byte| !(byte == b'=' || byte.is_ascii_whitespace()));
    re_encode(&vec).unwrap_or(vec)
}

fn re_encode(input: &[u8]) -> Result<Vec<u8>, ReEncodeError> {
    let decoded = BASE64_STANDARD_NO_PAD
        .decode(input)
        .map_err(|_| ReEncodeError::Base64)?;
    let structure: Property = serde_json::from_slice(&decoded).map_err(|_| ReEncodeError::Json)?;
    let json = serde_json::to_vec(&structure).map_err(|_| ReEncodeError::Json)?;
    let length = base64::encoded_len(json.len(), false).ok_or(ReEncodeError::Base64)?;
    let mut encoded = vec![u8::default(); length];
    let _ = BASE64_URL_SAFE_NO_PAD
        .encode_slice(json, &mut encoded)
        .map_err(|_| ReEncodeError::Base64)?;
    Ok(encoded)
}

#[derive(Debug)]
enum ReEncodeError {
    Base64,
    Json,
}

#[derive(Debug, Deserialize, Serialize)]
struct Property {
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
