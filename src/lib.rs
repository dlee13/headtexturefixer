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
    let input = str::from_utf8_unchecked(buffer);
    let output = strip(input);
    buffer[..output.len()].copy_from_slice(output.as_bytes());
    output.len()
}

fn strip(input: &str) -> String {
    let mut string = input.to_owned();
    string.retain(|character: char| !(character == '=' || character.is_whitespace()));
    re_encode(&string).unwrap_or(string)
}

fn re_encode(input: &str) -> Result<String, ReEncodeError> {
    let decoded = BASE64_STANDARD_NO_PAD
        .decode(input)
        .map_err(|_| ReEncodeError::Base64)?;
    let structure: Property = serde_json::from_slice(&decoded).map_err(|_| ReEncodeError::Json)?;
    let json = serde_json::to_string(&structure).map_err(|_| ReEncodeError::Json)?;
    let encoded = BASE64_URL_SAFE_NO_PAD.encode(json);
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
