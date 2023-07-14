/*
 * Copyright 2020 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

export * from './core';
export * as hash from './hash/hashes';
export { HSMSigner, HSMSignerFactory, HSMSignerOptions } from './identity/hsmsigner';
export * as signers from './identity/signers';

import { checkpointers as coreCheckpointers } from './core';
import * as extendedCheckpointers from './checkpointers';
export const checkpointers = Object.assign({}, coreCheckpointers, extendedCheckpointers);
