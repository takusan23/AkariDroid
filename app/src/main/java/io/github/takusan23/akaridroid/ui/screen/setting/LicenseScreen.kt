package io.github.takusan23.akaridroid.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/**
 * ライセンス情報データクラス
 * @param name 名前
 * @param license ライセンス
 * */
private data class LicenseData(
    val name: String,
    val license: String,
)

/** akari-core */
private val akariCore = LicenseData(
    name = "takusan23/AkariDroid akari-core",
    license = """
            --- akari-core ---
            
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
            
                http://www.apache.org/licenses/LICENSE-2.0
            
            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
            
            --- waywardgeek/sonic ---
            
            Sonic library
            Copyright 2010, 2011
            Bill Cox
            This file is part of the Sonic Library.
            
            This file is licensed under the Apache 2.0 license.
            
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
                http://www.apache.org/licenses/LICENSE-2.0
            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
            
            --- ypresto/qtfaststart-java ---
            
            The MIT License (MIT)
            
            Copyright (c) 2014 Yuya Tanaka
            
            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:
            
            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.
            
            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
            SOFTWARE.
            
            --- Android Open Source Project ---
            
            Copyright (C) 2013 The Android Open Source Project
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
            
                 http://www.apache.org/licenses/LICENSE-2.0
            
            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
    """.trimIndent()
)

private val kotlinCoroutine = LicenseData(
    name = "Kotlin/kotlinx.coroutines",
    license = """
           Copyright 2000-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.

           Licensed under the Apache License, Version 2.0 (the "License");
           you may not use this file except in compliance with the License.
           You may obtain a copy of the License at

               http://www.apache.org/licenses/LICENSE-2.0

           Unless required by applicable law or agreed to in writing, software
           distributed under the License is distributed on an "AS IS" BASIS,
           WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
           See the License for the specific language governing permissions and
           limitations under the License.
    """.trimIndent()
)

private val kotlinSerialization = LicenseData(
    name = "Kotlin/kotlinx.serialization",
    license = """
           Licensed under the Apache License, Version 2.0 (the "License");
           you may not use this file except in compliance with the License.
           You may obtain a copy of the License at

               http://www.apache.org/licenses/LICENSE-2.0

           Unless required by applicable law or agreed to in writing, software
           distributed under the License is distributed on an "AS IS" BASIS,
           WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
           See the License for the specific language governing permissions and
           limitations under the License.
    """.trimIndent()
)

private val materialIcons = LicenseData(
    name = "google/material-design-icons",
    license = """
           Licensed under the Apache License, Version 2.0 (the "License");
           you may not use this file except in compliance with the License.
           You may obtain a copy of the License at

               http://www.apache.org/licenses/LICENSE-2.0

           Unless required by applicable law or agreed to in writing, software
           distributed under the License is distributed on an "AS IS" BASIS,
           WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
           See the License for the specific language governing permissions and
           limitations under the License.
    """.trimIndent()
)

private val glide = LicenseData(
    name = "bumptech/glide",
    license = """
        License for everything not in third_party and not otherwise marked:

        Copyright 2014 Google, Inc. All rights reserved.

        Redistribution and use in source and binary forms, with or without modification, are
        permitted provided that the following conditions are met:

           1. Redistributions of source code must retain the above copyright notice, this list of
                 conditions and the following disclaimer.

           2. Redistributions in binary form must reproduce the above copyright notice, this list
                 of conditions and the following disclaimer in the documentation and/or other materials
                 provided with the distribution.

        THIS SOFTWARE IS PROVIDED BY GOOGLE, INC. ``AS IS'' AND ANY EXPRESS OR IMPLIED
        WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
        FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GOOGLE, INC. OR
        CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
        CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
        SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
        ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
        NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
        ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

        The views and conclusions contained in the software and documentation are those of the
        authors and should not be interpreted as representing official policies, either expressed
        or implied, of Google, Inc.
        ---------------------------------------------------------------------------------------------
        License for third_party/disklrucache:

        Copyright 2012 Jake Wharton
        Copyright 2011 The Android Open Source Project

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

           http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
        ---------------------------------------------------------------------------------------------
        License for third_party/gif_decoder:

        Copyright (c) 2013 Xcellent Creations, Inc.

        Permission is hereby granted, free of charge, to any person obtaining
        a copy of this software and associated documentation files (the
        "Software"), to deal in the Software without restriction, including
        without limitation the rights to use, copy, modify, merge, publish,
        distribute, sublicense, and/or sell copies of the Software, and to
        permit persons to whom the Software is furnished to do so, subject to
        the following conditions:

        The above copyright notice and this permission notice shall be
        included in all copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
        EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
        MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
        NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
        LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
        OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
        WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
        ---------------------------------------------------------------------------------------------
        License for third_party/gif_encoder/AnimatedGifEncoder.java and
        third_party/gif_encoder/LZWEncoder.java:

        No copyright asserted on the source code of this class. May be used for any
        purpose, however, refer to the Unisys LZW patent for restrictions on use of
        the associated LZWEncoder class. Please forward any corrections to
        kweiner@fmsware.com.

        -----------------------------------------------------------------------------
        License for third_party/gif_encoder/NeuQuant.java

        Copyright (c) 1994 Anthony Dekker

        NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994. See
        "Kohonen neural networks for optimal colour quantization" in "Network:
        Computation in Neural Systems" Vol. 5 (1994) pp 351-367. for a discussion of
        the algorithm.

        Any party obtaining a copy of these files from the author, directly or
        indirectly, is granted, free of charge, a full and unrestricted irrevocable,
        world-wide, paid up, royalty-free, nonexclusive right and license to deal in
        this software and documentation files (the "Software"), including without
        limitation the rights to use, copy, modify, merge, publish, distribute,
        sublicense, and/or sell copies of the Software, and to permit persons who
        receive copies from any such party to do so, with the only requirement being
        that this copyright notice remain intact.
    """.trimIndent()
)

/** ライセンス一覧 */
private val LicenseList = listOf(
    akariCore,
    kotlinCoroutine,
    kotlinSerialization,
    materialIcons,
    glide
)

/**
 * ライセンス画面。ありがとうございます。
 *
 * @param onBack 画面戻ってほしい時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.setting_license_title)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_arrow_back_24px),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(LicenseList) { licenseData ->
                LicenseItem(licenseData = licenseData)
                HorizontalDivider()
            }
        }
    }
}

/**
 * ライセンス一覧の各項目
 *
 * @param licenseData ライセンス情報
 * */
@Composable
private fun LicenseItem(licenseData: LicenseData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(5.dp),
            text = licenseData.name,
            fontSize = 25.sp
        )
        Text(
            text = licenseData.license,
            modifier = Modifier.padding(start = 5.dp, end = 5.dp)
        )
    }
}