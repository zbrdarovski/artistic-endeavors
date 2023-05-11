package si.um.feri.artisticendeavors

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.databinding.ActivityFullSizeImageBinding

class FullSizeImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullSizeImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullSizeImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("image_url")
        Picasso.get().load(imageUrl).into(binding.ivFullSizeImage)

        binding.btnClose.setOnClickListener {
            finish()
        }
    }
}