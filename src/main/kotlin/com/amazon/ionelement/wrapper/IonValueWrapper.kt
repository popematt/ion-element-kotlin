// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.ionelement.wrapper

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.*

/**
 * Implications:
 *   - IonElement now supports symbols with unknown text
 *   - ?
 *
 */
internal interface IonValueWrapper : IonElement {
    fun unwrap(): IonValue
}
