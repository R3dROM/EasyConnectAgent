package com.easyconnect.agent.pico

import android.util.Log
import com.easyconnect.agent.interfaces.IPicoFile
import com.pvr.tobservice.interfaces.IFileCopyCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoFileSystem(
    private val service: IToBServiceProxy
) : IPicoFile {
    private val fileCopyCallback = object : IFileCopyCallback.Stub()
    {
        override fun onCopyStart() {
            Log.d("FILE", "STARTING COPYING PROCESS")
        }

        override fun onCopyProgress(p0: Double) {
            Log.d("FILE", "COPYING FILES")
        }

        override fun onCopyFinish(p0: Int) {
            Log.d("FILE", "FINISH COPYING PROCESS")
        }

    }
    //FILE
    override suspend fun copy(from: String, to: String) : Int =
        service.fileCopy(
            from,
            to,
            fileCopyCallback
        )

}