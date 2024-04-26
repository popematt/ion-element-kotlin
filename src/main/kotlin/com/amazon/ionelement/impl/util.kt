// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal fun <T> Array<T>.toPersistentList(): PersistentList<T> = mapTo(persistentListOf<T>().builder()) { it }.build()
